/*
 * Copyright (c) 2017 Minio, Inc. <https://www.minio.io>
 *
 * This file is part of Alice.
 *
 * Alice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.minio.io.alice;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ServerUploadTask is an async tasker to upload to a remote object storage
 * server in background.
 */

public class ServerUploadTask extends AsyncTask<Void, Void, Void> {

    // Buffer to hold current image data.
    byte[] imageData;

    // Xray server response, contains the unmarshalled value
    // of presigned POST policy parameters.
    XrayResult serverResult;

    // Initialize a new http client per task.
    private final OkHttpClient client = new OkHttpClient();

    public ServerUploadTask(XrayResult xresult, byte[] imgData) {
        serverResult = xresult;
        imageData = imgData;
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected Void doInBackground(Void... params) {
        if (serverResult != null) {
            String url = serverResult.getPresignedURL();
            final JSONObject formData = serverResult.getPresignedFormData();

            if (formData != null) {
                final MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
                multipartBuilder.setType(MultipartBody.FORM);

                formData.keys().forEachRemaining(k ->
                {
                    try {
                        multipartBuilder.addFormDataPart(k, formData.getString(k));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });

                multipartBuilder.addFormDataPart("file", "alice.jpg",
                        RequestBody.create(null, imageData));
                RequestBody requestBody = multipartBuilder.build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!response.isSuccessful()) try {
                    throw new IOException("Unexpected code " + response);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Reset the image data buffer after successful upload to
                // relinquish the memory.
                imageData = null;
            }
        }
        return null;
    }
}
