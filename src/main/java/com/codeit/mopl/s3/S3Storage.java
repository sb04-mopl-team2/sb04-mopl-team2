package com.codeit.mopl.s3;

import com.codeit.mopl.exception.user.ProfileDeleteFailException;
import com.codeit.mopl.exception.user.ProfileUploadFailException;
import com.codeit.mopl.exception.user.UserErrorCode;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Storage {
    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${mopl.storage.s3.bucket}")
    private String bucket;

    @Retryable(
            retryFor = {ProfileUploadFailException.class},
            recover = "recover",
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void upload(MultipartFile file,String key) {
        log.info("[사용자 관리] 프로필 업로드 실행 fileKey = {}", key);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
            log.info("S3 업로드 성공");
        } catch (AwsServiceException e) {
            throw new ProfileUploadFailException(UserErrorCode.PROFILE_UPLOAD_FAIL, Map.of("key", key,"exceptionName",e.getClass().getSimpleName(),"exception",e));
        } catch (SdkClientException e) {
            throw new ProfileUploadFailException(UserErrorCode.PROFILE_UPLOAD_FAIL, Map.of("key", key,"exceptionName",e.getClass().getSimpleName(),"exception",e));
        } catch (Exception e) {
            throw new ProfileUploadFailException(UserErrorCode.PROFILE_UPLOAD_FAIL, Map.of("key", key,"exceptionName",e.getClass().getSimpleName(),"exception",e));
        }
    }

    public void delete(String profileImageUrl) {
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(profileImageUrl)
                .build();

        try {
            s3Client.deleteObject(delReq);
        } catch (Exception e) {
            throw new ProfileDeleteFailException(UserErrorCode.PROFILE_DELETE_FAIL, Map.of("imageUrl",profileImageUrl));
        }
    }

    private S3Client getS3Client() {
        return s3Client;
    }

    public String getPresignedUrl(String key) {
        String contentType = getContentTypeFromS3(key);

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentType(contentType)
                .build();

        GetObjectPresignRequest preReq = GetObjectPresignRequest.builder()
                .getObjectRequest(getReq)
                .signatureDuration(Duration.ofHours(1))
                .build();

        return presigner.presignGetObject(preReq).url().toString();
    }

    @Recover
    public void recover(ProfileUploadFailException e, String key) {
        log.info("S3 업로드 실패");
        throw new ProfileUploadFailException(UserErrorCode.PROFILE_UPLOAD_FAIL, Map.of("key",key));
    }

    private String getContentTypeFromS3(String key) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        HeadObjectResponse headResponse = s3Client.headObject(headRequest);
        return headResponse.contentType();
    }
}
