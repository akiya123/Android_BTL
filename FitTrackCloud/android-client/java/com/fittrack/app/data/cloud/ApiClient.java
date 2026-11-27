package com.fittrack.app.data.cloud;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private ApiClient() {}

    /**
     * Thiết bị thật / production (BẮT BUỘC https, xem mục "Cleartext" trong hướng dẫn):
     *   "https://api.ten-mien-cua-ban.com/api/v1/"
     * Android Emulator gọi API chạy trên máy tính:
     *   "http://10.0.2.2:5080/api/v1/"
     * Phải kết thúc bằng dấu "/".
     */
    public static final String BASE_URL = "https://api.example.com/api/v1/";

    private static volatile CloudSaveApi api;

    public static CloudSaveApi get() {
        if (api == null) {
            synchronized (ApiClient.class) {
                if (api == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(90, TimeUnit.SECONDS)   // snapshot lớn tải/up mất thời gian
                            .writeTimeout(90, TimeUnit.SECONDS)
                            .addInterceptor(new FirebaseTokenInterceptor())
                            .build();

                    api = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(CloudSaveApi.class);
                }
            }
        }
        return api;
    }

    /**
     * Gắn "Authorization: Bearer <Firebase ID token>" vào mọi request.
     * Interceptor chạy trên luồng nền của OkHttp nên được phép chặn chờ Task của Firebase.
     * Token tự làm mới khi hết hạn (1 giờ); nếu server vẫn trả 401 thì ép làm mới 1 lần rồi thử lại.
     */
    private static final class FirebaseTokenInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(withToken(chain.request(), false));
            if (response.code() == 401) {
                response.close();
                response = chain.proceed(withToken(chain.request(), true));
            }
            return response;
        }

        private Request withToken(Request original, boolean forceRefresh) throws IOException {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) throw new IOException("NOT_LOGGED_IN");
            try {
                String token = Tasks.await(user.getIdToken(forceRefresh), 15, TimeUnit.SECONDS).getToken();
                return original.newBuilder().header("Authorization", "Bearer " + token).build();
            } catch (Exception e) {
                throw new IOException("Không lấy được Firebase token: " + e.getMessage(), e);
            }
        }
    }
}
