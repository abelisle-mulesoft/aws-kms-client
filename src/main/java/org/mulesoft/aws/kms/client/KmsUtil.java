package org.mulesoft.aws.kms.client;

import com.amazonaws.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Utility class for encrypting and decrypting data using the AWS Key Management Service (KMS).
 * <p>
 * <b>Remark:</b> The current revision is a proof of concept, and it is not ready for production.
 * For example, exception handling is improperly implemented.
 *
 * @author  Alan Belisle
 */
public class KmsUtil {
    private static final Logger logger = LoggerFactory.getLogger(KmsUtil.class);

    // Default plain text used for unit testing the encrypt and decrypt methods.
    private static final String DEFAULT_TEXT_TO_ENCRYPT = "A simple string for testing purposes";

    // Default filename of the AWS-specific property file.
    private static final String AWS_CONFIG_PROPERTY_FILENAME = "aws_config.properties";

    // Holds AWS-specific property list - AWS key pair for example.
    private static Properties awsConfigProps = null;

    // Default filename of the AWS KMS-specific property file.
    private static final String AWS_KMS_CONFIG_PROPERTY_FILENAME = "aws_kms_config.properties";

    // Holds AWS KMS-specific property list - Region and Key ARN for example.
    private static Properties awsKmsConfigProps = null;

    /**
     * The main method is implemented for the sole purpose of unit testing the encrypt and decrypt methods.
     * If command-line arguments are provided, the main method encrypts the first one and then decrypts it.
     * @param args Optional - A string array containing the command-line arguments.
     */
    public static void main(String[] args) {
        String textToEncrypt;

        // Use first command-line argument for unit testing if any are provided.
        if (args.length > 0) {
            textToEncrypt = args[0];
        } else {
            textToEncrypt = DEFAULT_TEXT_TO_ENCRYPT;
        }

        try {
            logger.info("Encrypting: '{}'", textToEncrypt);
            String encryptedText = encrypt(textToEncrypt);
            logger.info("Encrypted value: {}", encryptedText);

            logger.info("Decrypting");
            String decryptedText = decrypt(encryptedText);
            logger.info("Decrypted value: {}", decryptedText);
        } catch (Exception e) {
            // Do nothing
        }
    }

    /**
     * Decrypts the provided string using the AWS KMS.
     * @param textToDecrypt The Base64 encoded text to decrypt.
     * @return The decrypted string in plain text.
     * @throws Exception If any exception occurred.
     */
    public static String decrypt(String textToDecrypt) throws Exception {
        logger.debug("decrypt - Starting");

        String decryptedText = null;

        // First, connect to the KMS
        KmsClient kmsClient = kmsConnect();

        // Convert the encrypted text (in Base64 encoded format) provided to the format expected by the AWS KMS
        SdkBytes sdkBytesToDecrypt = SdkBytes.fromByteArray(Base64.decode(textToDecrypt));

        try {
            // Create a DecryptRequest object with the SdkBytes/converted encrypted text.
            DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(sdkBytesToDecrypt).build();

            // Call AWS KMS to perform the decryption.
            DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);

            // Extract and convert the decrypted text to a String.
            decryptedText = new String(decryptResponse.plaintext().asByteArray());

            // Close the connection to AWS KMS
            kmsClient.close();
        } catch (Exception e) {
            logger.error("Failed to decrypt", e);
        }

        logger.debug("decrypt - Ending");
        return decryptedText;
    }

    /**
     * Encrypts the provided string using the AWS KMS.
     * @param textToEncrypt The plain text to encrypt.
     * @return The encrypted text as a Base64 encoded string.
     * @throws Exception If any exception occurred.
     */
    public static String encrypt(String textToEncrypt) throws Exception {
        logger.debug("encrypt - Starting");

        String encryptedText = null;

        // First, connect to the KMS
        KmsClient kmsClient = kmsConnect();

        // Convert the plain text provided to the format expected by the AWS KMS
        SdkBytes sdkBytesToEncrypt = SdkBytes.fromByteArray(textToEncrypt.getBytes());

        // Get the Amazon Resource Name (ARN) of the encryption key to use from the AWS KMS-specific property list
        String awsKmsKeyArn = getAwsKmsConfigProperty("aws_kms_key_arn");

        try {
            // Create an EncryptRequest object with the encryption key ARN and SdkBytes/converted plain text.
            EncryptRequest encryptRequest = EncryptRequest.builder().keyId(awsKmsKeyArn).plaintext(sdkBytesToEncrypt).build();

            // Call AWS KMS to perform the encryption.
            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);

            // Extract and convert the encrypted text to a Base64 encoded String.
            encryptedText = new String(Base64.encode(encryptResponse.ciphertextBlob().asByteArray()));

            // Close the connection to AWS KMS
            kmsClient.close();
        } catch (Exception e) {
            logger.error("Failed to encrypt", e);
        }

        logger.debug("encrypt - Ending");
        return encryptedText;
    }

    /**
     * Convenience method for connecting to AWS and AWS KMS.
     * @return A client connection to AWS KMS.
     * @throws Exception If any exception occurred.
     */
    private static KmsClient kmsConnect() throws Exception {
        KmsClient kmsClient;

        // Get the key pair to use for connecting to AWS KMS
        String awsKeyId = getAwsConfigProperty("aws_access_key_id");
        String awsAccessKey = getAwsConfigProperty("aws_secret_access_key");

        // Get the key pair to use for connecting to AWS KMS
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsKeyId, awsAccessKey);

        // Get the preferred AWS region to use.
        Region awsRegion = Region.of(getAwsKmsConfigProperty("aws_kms_region"));

        // Connect to AWS KMS with the basic credentials and the preferred region.
        kmsClient = KmsClient.builder().credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).region(awsRegion).build();
        logger.debug("Connected to AWS KMS in the " + awsRegion.toString() + " region");

        return kmsClient;
    }

    /**
     * Convenience method to search for the property with the specified key within the AWS-specific properties.
     * It loads in memory the properties file on first use.
     * @param key The property key.
     * @return The value in the AWS-specific property list with the specified key value.
     * @throws Exception If any exception occurred.
     */
    private static String getAwsConfigProperty(String key) throws Exception {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();

        // First, loads the properties file in memory on first use.
        if (awsConfigProps == null) {
            String awsConfigPath = rootPath + "/" + AWS_CONFIG_PROPERTY_FILENAME;
            try {
                awsConfigProps = new Properties();
                awsConfigProps.load(new FileInputStream(awsConfigPath));
            } catch (Exception e) {
                logger.error("Failed to load {}", AWS_CONFIG_PROPERTY_FILENAME, e);
                throw e;
            }
        }
        // Simply return the value of the specified key value.
        return awsConfigProps.getProperty(key);
    }

    /**
     * Convenience method to search for the property with the specified key within the AWS KMS-specific properties.
     * It loads in memory the properties file on first use.
     * @param key The property key.
     * @return The value in the AWS KMS-specific property list with the specified key value.
     * @throws Exception If any exception occurred.
     */
    private static String getAwsKmsConfigProperty(String key) throws Exception {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();

        // First, loads the properties file in memory on first use.
        if (awsKmsConfigProps == null) {
            String awsKmsConfigPath = rootPath + "/" + AWS_KMS_CONFIG_PROPERTY_FILENAME;

            try {
                awsKmsConfigProps = new Properties();
                awsKmsConfigProps.load(new FileInputStream(awsKmsConfigPath));
            } catch (Exception e) {
                logger.error("Failed to load {}", AWS_KMS_CONFIG_PROPERTY_FILENAME, e);
                throw e;
            }
        }
        // Simply return the value of the specified key value.
        return awsKmsConfigProps.getProperty(key);
    }
}
