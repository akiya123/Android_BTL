package com.fittrack.app.data.cloud;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Retrofit interface. BASE_URL kết thúc bằng "/api/v1/" nên đường dẫn ở đây KHÔNG có dấu "/" ở đầu. */
public interface CloudSaveApi {

    @GET("cloud-save")
    Call<List<CloudModels.BackupInfo>> list();

    /** 404 = cloud chưa có bản sao lưu nào. */
    @GET("cloud-save/latest")
    Call<CloudModels.BackupInfo> latestInfo();

    @GET("cloud-save/latest/data")
    Call<CloudModels.Download> downloadLatest();

    @GET("cloud-save/{id}/data")
    Call<CloudModels.Download> download(@Path("id") long id);

    /** 201 created | 200 unchanged | 409 conflict (đọc errorBody -> ConflictResponse). */
    @POST("cloud-save")
    Call<CloudModels.UploadResponse> upload(@Body CloudModels.UploadRequest body);

    @DELETE("cloud-save/{id}")
    Call<Void> delete(@Path("id") long id);

    /** Xóa vĩnh viễn toàn bộ dữ liệu cloud của tài khoản này. */
    @DELETE("me")
    Call<Void> deleteAllMyCloudData();
}
