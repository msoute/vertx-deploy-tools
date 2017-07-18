package nl.jpoint.maven.vertx.state;

import com.amazonaws.services.s3.AmazonS3Encryption;
import nl.jpoint.maven.vertx.request.DeployRequest;

public class S3DeployState {

    private final AmazonS3Encryption s3Client;

    public S3DeployState(AmazonS3Encryption s3Client) {
        this.s3Client = s3Client;
    }

    public void store(DeployRequest request) {
        //  s3Client.putObject();
    }

    public void retreive() {

    }
}
