package com.neu.csye6225.serverless;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.util.Arrays;
import java.util.List;

public class EmailNotification implements RequestHandler<SNSEvent, Object> {

    private DynamoDB dynamoDB;
    private static String EMAIL_SUBJECT = "Book ";
    private static final String EMAIL_SENDER = "no-reply@prod.tianyubai.me";

    public Object handleRequest(SNSEvent request, Context context){
        // confirm dynamoDB table exists
        dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        Table table = dynamoDB.getTable("Emails_Sent");
        if(table == null) {
            context.getLogger().log("Table 'Emails_Sent' is not in dynamoDB.");
            return null;
        } else if (request.getRecords() == null) {
            context.getLogger().log("There are currently no records in the SNS Event.");
            return null;
        }

        // get SNS message
        String msgSNS =  request.getRecords().get(0).getSNS().getMessage();
        // requestType, recipientEmail, bookId, bookName, author, link
        List<String> msgInfo = Arrays.asList(msgSNS.split("\\|"));
        StringBuilder emailMsg = new StringBuilder();
        for (int i = 2; i < msgInfo.size() - 1; i++) { emailMsg.append(msgInfo.get(i)).append("\n"); }
        if (msgInfo.get(0).equals("POST")) {
            emailMsg.append("Full details of the book can be viewed at: ").append(msgInfo.get(5));
            emailMsg.insert(0, "You have successfully added the following book.\n");
            EMAIL_SUBJECT += "Added";
        } else {
            emailMsg.insert(0, "You have successfully deleted the following book.\n");
            EMAIL_SUBJECT += "Deleted";
        }
        
        // send email if no duplicate in dynamoDB
        Item item = table.getItem("id", emailMsg);
        if (item == null) {
            table.putItem(new PutItemSpec().withItem(new Item().withString("id", emailMsg.toString())));
            Content content = new Content().withData(emailMsg.toString());
            Body emailBody = new Body().withText(content);
            try {
                AmazonSimpleEmailService emailService =
                        AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
                SendEmailRequest emailRequest = new SendEmailRequest()
                        .withDestination(new Destination().withToAddresses(msgInfo.get(1)))
                        .withMessage(new Message().withBody(emailBody)
                                .withSubject(new Content().withCharset("UTF-8").withData(EMAIL_SUBJECT)))
                        .withSource(EMAIL_SENDER);
                emailService.sendEmail(emailRequest);
            } catch (Exception ex) {}
        }

        return null;
    }
}
