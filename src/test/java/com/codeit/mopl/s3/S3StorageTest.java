package com.codeit.mopl.s3;

import com.codeit.mopl.exception.user.ProfileDeleteFailException;
import com.codeit.mopl.exception.user.ProfileUploadFailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class S3StorageTest {
    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner presigner;

    @InjectMocks
    private S3Storage s3Storage;

    @DisplayName("s3Client.putObject는 1번만 동작한다")
    @Test
    void s3UploadSucceed() throws Exception {
        MockMultipartFile profile = new MockMultipartFile("image","originalName", MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        String key = UUID.randomUUID().toString();
        s3Storage.upload(profile,key);

        then(s3Client).should(times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @DisplayName("AwsService 오류가 발생하면 ProfileUploadFailException으로 감싸서 처리한다.")
    @Test
    void s3UploadFailAwsServiceException() throws Exception {
        String key = UUID.randomUUID().toString();
        MockMultipartFile profile = new MockMultipartFile("image","originalName", MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        doThrow(AwsServiceException.class).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        ProfileUploadFailException exception = assertThrows(ProfileUploadFailException.class, () -> s3Storage.upload(profile,key));

        assertEquals("AwsServiceException", exception.getDetails().get("exceptionName"));
    }

    @DisplayName("SDKClient 오류가 발생하면 ProfileUploadFailException으로 감싸서 처리한다.")
    @Test
    void s3UploadFailSdkClientException() throws Exception {
        String key = UUID.randomUUID().toString();
        MockMultipartFile profile = new MockMultipartFile("image","originalName", MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        doThrow(SdkClientException.class).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        ProfileUploadFailException exception = assertThrows(ProfileUploadFailException.class, () -> s3Storage.upload(profile,key));

        assertEquals("SdkClientException", exception.getDetails().get("exceptionName"));
    }

    @DisplayName("이미지 이름(Key)가 주어지면 presignedUrl이 반환된다.")
    @Test
    void getPresignedUrlSucceed() throws Exception {
        String key = UUID.randomUUID().toString();
        String presignedUrl = "http://test-bucket.com/";

        PresignedGetObjectRequest req = Mockito.mock(PresignedGetObjectRequest.class);
        given(req.url()).willReturn(URI.create(presignedUrl).toURL());
        given(presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(req);
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder().contentType(MediaType.IMAGE_JPEG_VALUE).build());

        String getPresignedUrl = s3Storage.getPresignedUrl(key);

        assertEquals(presignedUrl, getPresignedUrl);
    }

    @DisplayName("프로필 URL이 주어지면 삭제한다")
    @Test
    void deleteImageSucceed() throws Exception {
        String key = UUID.randomUUID().toString();
        DeleteObjectResponse res = Mockito.mock(DeleteObjectResponse.class);

        given(s3Client.deleteObject(any(DeleteObjectRequest.class))).willReturn(res);

        s3Storage.delete(key);

        then(s3Client).should(times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @DisplayName("프로필 이미지 삭제 중 오류가 발생하면 ProfileDeleteFailException을 반환한다")
    @Test
    void deleteImageFail()  throws Exception {
        String key = UUID.randomUUID().toString();
        doThrow(SdkClientException.class).when(s3Client).deleteObject(any(DeleteObjectRequest.class));
        ProfileDeleteFailException exception = assertThrows(ProfileDeleteFailException.class, () ->{
            s3Storage.delete(key);
        });

        assertEquals("프로필 삭제 실패", exception.getErrorCode().getMessage());
    }
}
