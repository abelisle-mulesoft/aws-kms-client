# AWS KMS Client Project

This repository contains a single Java class that leverages the AWS Key Management Service (KMS) API to perform encryption and decryption. We implemented this class to use it directly in [DataWeave scripts](https://docs.mulesoft.com/dataweave/2.4/dataweave-language-introduction) to support field-level encryption within the [MuleSoft Anypoint Platform](https://www.mulesoft.com/platform/enterprise-integration).

> :exclamation: **The current revision is a proof of concept, and it is not ready for production.** For examples, exception handling is improperly implemented, and we tested using a local Maven repository instead of Anypoint Exchange.

## Background
We completed this proof of concept following a request from a customer who asked us for suggested approaches and options to encrypt and decrypt personal identifiable information (PII) stored in a database. The primary aspects of their request were:
1. Data at rest encryption for specific fields, 
2. Ideally leveraging the AWS Key Management Service (KMS) to manage the encryption key, and
3. All done within the MuleSoft Anypoint Platform.

## Instructions
1. Clone this repo.
2. Edit `src/main/resources/aws_config.properties` and enter:
   1. Your AWS access key, and 
   2. Your AWS secret key.
3. Edit `src/main/resources/aws_kms_config.properties` and enter:
   1. Your preferred AWS region, and
   2. The Amazon Resource Name (ARN) of your AWS KMS key.
4. Optionally, edit `src/main/resources/logback.xml` to change the logging configuration.
5. Install this project into your Maven local repository:

    ```sh
    mvn clean compile
    ```
6. In Anypoint Studio, add this project as a dependency for your Mule application. For example:

    ```xml
   <dependency>
       <groupId>org.mulesoft.aws.kms</groupId>
       <artifactId>AWS-KMS-Client</artifactId>
       <version>1.0.0</version>
       <scope>import</scope>
   </dependency>
    ```
## DataWeave Script Example
As an example, consider an API that posts employee data to a database using the following (somewhat simplistic) Employee data type.

```json
{
  "employeeNumber": "50-2263455",
  "firstName": "Fiona",
  "middleName": "Renee",
  "lastName": "Howell",
  "homeStreet": "31 Redwing Park",
  "homeCity": "Stamford",
  "homeState": "CT",
  "homeCountry": "United States",
  "homePostalCode": "06912",
  "homePhone": "2037915049",
  "workPhone": "2034164053",
  "email": "frhowell@mail.com",
  "dateOfBirth": "1984-06-26",
  "socialSecurityNumber": "506-42-6579",
  "currentWellnessStatus": "Available To Work",
  "employeeStatus": "Active",
  "statusAsOf": "2020-05-04",
  "employmentType": "Full-Time",
  "gender": "Female",
  "locationId": "3213",
  "managerId": "56-7998565",
  "workerType": "Employee"
}
```

And following is a sample DataWeave script for encrypting personal identifiable information (PII) using this AWS KMS Client Project.

```text
%dw 2.0
import java!org::mulesoft::aws::kms::client::KmsUtil

output application/java
---
{
	employeeNumber: payload.employeeNumber,
	firstName: payload.firstName,
	middleName: payload.middleName,
	lastName: payload.lastName,
	homeStreet: KmsUtil::encrypt(payload.homeStreet),
	homeCity: KmsUtil::encrypt(payload.homeCity),
	homeState: KmsUtil::encrypt(payload.homeState),
	homeCountry: KmsUtil::encrypt(payload.homeCountry),
	homePostalCode: KmsUtil::encrypt(payload.homePostalCode),
	homePhone: KmsUtil::encrypt(payload.homePhone),
	workPhone: payload.workPhone,
	email: payload.workEmail,
	dateOfBirth: KmsUtil::encrypt(payload.dateOfBirth),
	ssn: KmsUtil::encrypt(payload.socialSecurityNumber),
	currentWellnessStatus: payload.currentWellnessStatus,
	employeeStatus: payload.employeeStatus,
	statusAsOf: payload.statusAsOf as Date,
	employmentType: payload.employmentType,
	gender: payload.gender,
	locationId: payload.locationId,
	managerId: payload.managerId,
	workerType: payload.workerType
}
```

## Reporting Issues

You can report new issues at this link https://github.com/abelisle-mulesoft/aws-kms-client/issues.

## Resources
- AWS Key Management Service (KMS) extension for Mule 4: https://github.com/mulesoft-consulting/mule4-aws-kms-module
- Programming the AWS KMS API: https://docs.aws.amazon.com/kms/latest/developerguide/programming-top.html
- AWS KMS Encrypt: AWS KMS Encrypt: https://docs.aws.amazon.com/kms/latest/APIReference/API_Encrypt.html
- AWS KMS Decrypt: AWS KMS Decrypt: https://docs.aws.amazon.com/kms/latest/APIReference/API_Decrypt.html
- Call Java Methods with DataWeave: https://docs.mulesoft.com/DataWeave/2.4/DataWeave-cookbook-java-methods
