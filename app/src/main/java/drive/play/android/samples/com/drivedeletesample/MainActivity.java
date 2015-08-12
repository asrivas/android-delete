/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package drive.play.android.samples.com.drivedeletesample;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Google Drive Android API Delete sample.
 *
 * This sample demonstrates the deletion of app data using the DriveResource.delete() method.
 * While the delete method can be used to delete any resource, the DriveResource.trash() method
 * is recommended for user visible files as trashing gives users the ability to recover files.
 * Deletion is not recoverable.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * AsyncTask to submit answers to sums by writing them to an app data file.
     * If the file exists it is updated, otherwise the file is created then written to.
     */
    private class SubmitAnswerAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final String driveIdStr = sharedPreferences.getString(DRIVE_ID, null);
            if (driveIdStr != null) {
                // Open app data file if already exists.
                DriveId fileId = DriveId.decodeFromString(driveIdStr);
                DriveFile sumFile = fileId.asDriveFile();
                DriveApi.DriveContentsResult sumContentsResult =
                        sumFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY,
                                null).await();
                if (!sumContentsResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Unable to retrieve contents.");
                    return null;
                }
                updateSums(sumContentsResult.getDriveContents());
            } else {
                // Create app data file if it does not exist.
                DriveApi.DriveContentsResult sumContentsResult =
                        Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
                if (!sumContentsResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Unable to retrieve contents.");
                    return null;
                }
                writeFirstSum(sumContentsResult.getDriveContents());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mSubmitButton.setEnabled(true);
        }
    }

    /**
     * AsyncTask to delete app data file.
     */
    private class ResetAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final String driveIdStr = sharedPreferences.getString(DRIVE_ID, null);
            if (driveIdStr != null) {
                DriveId fileId = DriveId.decodeFromString(driveIdStr);
                DriveFile sumFile = fileId.asDriveFile();
                // Call to delete app data file. Consider using DriveResource.trash()
                // for user visible files.
                com.google.android.gms.common.api.Status deleteStatus =
                        sumFile.delete(mGoogleApiClient).await();
                if (!deleteStatus.isSuccess()) {
                    Log.e(TAG, "Unable to delete app data.");
                    return null;
                }
                // Remove stored DriveId.
                sharedPreferences.edit().remove(DRIVE_ID).apply();
                Log.d(TAG, "Past sums deleted.");
            }
            return null;
        }
    }

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String DRIVE_ID = "driveId";

    /**
     * GoogleApiClient wraps our service connection to Google Play Services and provides access
     * to Google APIs.
     */
    private GoogleApiClient mGoogleApiClient;

    private SharedPreferences sharedPreferences;
    private ArrayAdapter<String> mSumAdapter;

    private Sum currentSum;
    private TextView questionTextView;
    private EditText answerEditText;
    private Button mSubmitButton;
    private Button mResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ListView listView = (ListView) findViewById(R.id.listView);
        mSumAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        listView.setAdapter(mSumAdapter);

        questionTextView = (TextView) findViewById(R.id.questionTextView);
        answerEditText = (EditText) findViewById(R.id.answerEditText);
        showSum();

        // Submit an answer to the current sum.
        mSubmitButton = (Button) findViewById(R.id.submitButton);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (answerEditText.getText().length() == 0) {
                    return;
                }
                mSubmitButton.setEnabled(false);
                currentSum.setAns(Integer.parseInt(answerEditText.getText().toString()));
                mSumAdapter.insert(currentSum.toString(), 0);
                mSumAdapter.notifyDataSetChanged();
                // Show new sum.
                showSum();

                // Save the current sum to app data.
                new SubmitAnswerAsyncTask().execute();
            }
        });

        // Delete app data file.
        mResetButton = (Button) findViewById(R.id.resetButton);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSumAdapter.clear();
                mSumAdapter.notifyDataSetChanged();

                new ResetAsyncTask().execute();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addScope(Drive.SCOPE_APPFOLDER)
                .build();

        mGoogleApiClient.connect();
    }

    /**
     * Create and show a math sum.
     */
    private void showSum() {
        currentSum = new Sum();
        questionTextView.setText(currentSum.getP1() + " " + currentSum.getOperation() + " "
                + currentSum.getP2());
        answerEditText.setText("");
    }

    /**
     * Use given DriveContents to create an app data file and write the first sum to it.
     *
     * @param driveContents
     */
    private void writeFirstSum(final DriveContents driveContents) {
        writeSums(driveContents);

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("Sum File")
                .setMimeType("text/plain")
                .build();

        DriveFolder.DriveFileResult driveFileResult = Drive.DriveApi
                .getAppFolder(mGoogleApiClient)
                .createFile(mGoogleApiClient, changeSet, driveContents)
                .await();

        if (!driveFileResult.getStatus().isSuccess()) {
            Log.e(TAG, "Unable to create sum file.");
            return;
        }
        storeSumFileId(driveFileResult.getDriveFile().getDriveId());
        Log.d(TAG, "App data successfully written. DriveId stored in shared preferences.");
    }

    /**
     * Use the given DriveContents to update app data file with current sum.
     *
     * @param driveContents
     */
    private void updateSums(DriveContents driveContents) {
        writeSums(driveContents);
        com.google.android.gms.common.api.Status writeStatus =
                driveContents.commit(mGoogleApiClient, null).await();
        if (!writeStatus.isSuccess()) {
            Log.e(TAG, "Unable to write contents.");
            return;
        }
        Log.d(TAG, "App data successfully updated.");
    }

    /**
     * Write the current sum to the given DriveContents' OutputStream.
     *
     * @param driveContents
     */
    private void writeSums(final DriveContents driveContents) {
        OutputStream outputStream = driveContents.getOutputStream();
        Writer writer = new OutputStreamWriter(outputStream);
        try {
            for (int i = 0; i < mSumAdapter.getCount(); i++) {
                writer.write(mSumAdapter.getItem(i) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiate loading of math sums from app data.
     */
    private void loadPastSums() {
        // Using AsyncTask here to load past sums off the UI thread.
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                DriveFolder appFolder = Drive.DriveApi.getAppFolder(mGoogleApiClient);
                // In a real world application there may be many app data files. A more specific
                // query could be used here to return a smaller number of files (if not one) if
                // many app data files exist.
                DriveApi.MetadataBufferResult result = appFolder.listChildren(mGoogleApiClient)
                        .await();
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "Unable to retrieve app data file list.");
                    return null;
                }

                List<String> sums = new ArrayList<>();
                // If there already exists app data, use it to load past sums.
                Log.d(TAG, result.getMetadataBuffer().getCount() + "");
                if (result.getMetadataBuffer().getCount() > 0) {
                    DriveId fileId = result.getMetadataBuffer().get(0).getDriveId();
                    DriveFile sumFile = fileId.asDriveFile();

                    // Once the open is complete, retrieve DriveContents.
                    DriveApi.DriveContentsResult sumContentsResult = sumFile
                            .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
                    if (!sumContentsResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Unable to retrieve file contents.");
                        return null;
                    }

                    // Retrieve InputStream of DriveContents and use it to extract sums. Each
                    // sum is stored on a separate line.
                    DriveContents sumContents = sumContentsResult.getDriveContents();
                    Scanner scanner = new Scanner(sumContents.getInputStream());

                    while (scanner.hasNextLine()) {
                        String sumLine = scanner.nextLine();
                        sums.add(sumLine);
                    }

                }
                return sums;
            }

            @Override
            protected void onPostExecute(List<String> sums) {
                mSumAdapter.clear();
                for (int i = 0; i < sums.size(); i++) {
                    mSumAdapter.add(sums.get(i));
                }
                mSumAdapter.notifyDataSetChanged();
                Log.d(TAG, "Past sums loaded.");
            }

        }.execute();
    }

    /**
     * Save the DriveId of the app data file used to store the math sums.
     *
     * @param driveId
     */
    private void storeSumFileId(DriveId driveId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DRIVE_ID, driveId.encodeToString());
        editor.apply();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google API Client connected.");

        // Enable buttons that allow interaction with Google Drive Android API.
        mResetButton.setEnabled(true);
        mSubmitButton.setEnabled(true);

        // Load past sums.
        loadPastSums();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient suspended.");

        // Disable buttons that allow interaction with Google Drive Android API.
        mResetButton.setEnabled(false);
        mSubmitButton.setEnabled(false);
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but fails.
     * Handle {@code connectionResult.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0);
            return;
        }
        try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }
}
