/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package drive.play.android.samples.com.drivedeletesample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Google Drive Android API Delete sample.
 *
 * <p> This sample demonstrates the deletion of App Data using
 * {@link DriveResourceClient#delete(DriveResource)}. While the delete method <i>can</i> be used to
 * delete any resource, the {@link DriveResourceClient#trash(DriveResource)} method is recommended
 * for user visible files as trashing gives users the ability to recover files. Deletion is not
 * recoverable.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private static final int REQUEST_CODE_SIGN_IN = 0;
  private static final String DRIVE_ID = "driveId";

  /** Handles access to resources in Drive. */
  private DriveResourceClient mDriveResourceClient;

  private SharedPreferences mSharedPreferences;
  private ArrayAdapter<String> mSumAdapter;
  private Equation mCurrentEquation;

  /** UI elements. */
  private TextView mQuestionTextView;
  private EditText mAnswerEditText;
  private Button mSubmitButton;
  private Button mResetButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Create and add adapter to this activity's ListView.
    ListView listView = (ListView) findViewById(R.id.listView);
    mSumAdapter =
        new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
    listView.setAdapter(mSumAdapter);

    // Set up views and listeners for views. All views are disabled by default, and will be
    // enabled once sign-in is complete.
    mQuestionTextView = (TextView) findViewById(R.id.questionTextView);
    mAnswerEditText = (EditText) findViewById(R.id.answerEditText);
    showSum();

    // Set up button to submit an answer to the current sum.
    mSubmitButton = (Button) findViewById(R.id.submitButton);
    mSubmitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mAnswerEditText.getText().length() == 0) {
          return;
        }
        mCurrentEquation.setAnswer(Integer.parseInt(mAnswerEditText.getText().toString()));
        mSumAdapter.insert(mCurrentEquation.toString(), 0);
        mSumAdapter.notifyDataSetChanged();

        // Show new sum.
        showSum();

        // Save the current sum to app data.
        submitAnswer();
      }
    });

    // Set up button to delete App Data file.
    mResetButton = (Button) findViewById(R.id.resetButton);
    mResetButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mSumAdapter.clear();
        mSumAdapter.notifyDataSetChanged();

        deleteSavedEquations();
      }
    });

    // Initialize sign-in flow.
    signIn();
  }

  /**
   * Enables Views if sign-in was successful.
   */
  private void enableViews() {
    mQuestionTextView.setEnabled(true);
    mAnswerEditText.setEnabled(true);
    mSubmitButton.setEnabled(true);
    mResetButton.setEnabled(true);
  }

  /**
   * Creates and shows the math problem.
   */
  private void showSum() {
    mCurrentEquation = new Equation();
    mQuestionTextView.setText(mCurrentEquation.getP1() + " " + mCurrentEquation.getOperation() + " "
        + mCurrentEquation.getP2());
    mAnswerEditText.setText("");
  }

  /**
   * Writes the first sum to the specified {@code driveContents} and creates a new App Data file
   * from the contents.
   */
  private void writeFirstSum(final DriveContents driveContents) {
    writeSums(driveContents);

    final MetadataChangeSet changeSet =
        new MetadataChangeSet.Builder().setTitle("Equation File").setMimeType("text/plain").build();

    mDriveResourceClient.getAppFolder()
        .continueWithTask(new Continuation<DriveFolder, Task<DriveFile>>() {
          @Override
          public Task<DriveFile> then(@NonNull Task<DriveFolder> task) throws Exception {
            if (task.isSuccessful()) {
              DriveFolder appData = task.getResult();
              return mDriveResourceClient.createFile(appData, changeSet, driveContents);
            } else {
              Log.e(TAG, "Unable to retrieve App Data", task.getException());
              return Tasks.forException(task.getException());
            }
          }
        })
        .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
          @Override
          public void onSuccess(DriveFile driveFile) {
            storeSumFileId(driveFile.getDriveId());
            Log.d(TAG, "App data successfully written. DriveId stored in shared preferences");
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Unable to create sum file", e);
          }
        });
  }

  /**
   * Updates the App Data file associated with {@code driveContents} with the current sum.
   */
  private void updateSums(DriveContents driveContents) {
    writeSums(driveContents);

    mDriveResourceClient.commitContents(driveContents, /* metadataChangeSet= */ null)
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            Log.d(TAG, "App data successfully updated");
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Unable to write contents.", e);
          }
        });
  }

  /**
   * Writes the current sum to the specified {@code driveContents}.
   */
  private void writeSums(final DriveContents driveContents) {
    OutputStream outputStream = driveContents.getOutputStream();
    try (Writer writer = new OutputStreamWriter(outputStream)) {
      for (int i = 0; i < mSumAdapter.getCount(); i++) {
        writer.write(mSumAdapter.getItem(i) + "\n");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error occurred while writing to driveContents.", e);
    }
  }

  /**
   * Loads the past sums from the App Data file.
   */
  private void loadPastSums() {
    mDriveResourceClient.getAppFolder()
        .continueWithTask(new Continuation<DriveFolder, Task<MetadataBuffer>>() {
          @Override
          public Task<MetadataBuffer> then(@NonNull Task<DriveFolder> task) throws Exception {
            if (!task.isSuccessful()) {
              Log.e(TAG, "Unable to retrieve App Data folder.", task.getException());
              return Tasks.forException(task.getException());
            }

            DriveFolder appDataFolder = task.getResult();
            return mDriveResourceClient.listChildren(appDataFolder);
          }
        })
        .continueWithTask(new Continuation<MetadataBuffer, Task<DriveContents>>() {
          @Override
          public Task<DriveContents> then(@NonNull Task<MetadataBuffer> task) throws Exception {
            if (!task.isSuccessful()) {
              Log.e(TAG, "Unable to retrieve App Data file list.", task.getException());
              return Tasks.forException(task.getException());
            }

            // If there already exists an App Data file, use it to load past sums.
            MetadataBuffer metadataBuffer = task.getResult();
            Log.d(TAG, metadataBuffer.getCount() + "");
            if (metadataBuffer.getCount() > 0) {
              DriveId driveId = metadataBuffer.get(0).getDriveId();
              DriveFile sumFile = driveId.asDriveFile();

              // Once the open is complete, retrieve DriveContents
              return mDriveResourceClient.openFile(sumFile, DriveFile.MODE_READ_ONLY);
            } else {
              return null;
            }
          }
        })
        .addOnSuccessListener(new OnSuccessListener<DriveContents>() {
          @Override
          public void onSuccess(DriveContents driveContents) {
            mSumAdapter.clear();

            // Retrieve InputStream of DriveContents and use it to extract sums. Each sum is
            // stored on a separate line.
            if (driveContents != null) {
              Scanner scanner = new Scanner(driveContents.getInputStream());
              while (scanner.hasNextLine()) {
                mSumAdapter.add(scanner.nextLine());
              }
            }

            mSumAdapter.notifyDataSetChanged();
            Log.d(TAG, "Past sums loaded.");
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Unable to retrieve file contents.", e);
          }
        });
  }

  /**
   * Saves {@code driveId} of the associated App Data file to {@link SharedPreferences}.
   */
  private void storeSumFileId(DriveId driveId) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(DRIVE_ID, driveId.encodeToString());
    editor.apply();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE_SIGN_IN) {
      if (resultCode == RESULT_OK) {
        Log.i(TAG, "Signed in successfully");
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        if (task.isSuccessful()) {
          onSignInSuccess(task.getResult());
        } else {
          Log.e(TAG, "Unable to retrieve signed in account.", task.getException());
          finish();
        }
      } else {
        Log.e(TAG, "Unable to sign in, result code " + resultCode);
      }
    }
  }

  /**
   * Attempts to sign-in first via silent sign-in, then with a sign-in {@link Intent}.
   */
  private void signIn() {
    Log.i(TAG, "Start silent sign-in.");
    final GoogleSignInClient signInClient = buildGoogleSignInClient();
    signInClient.silentSignIn()
        .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
          @Override
          public void onSuccess(GoogleSignInAccount googleSignInAccount) {
            onSignInSuccess(googleSignInAccount);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            // Silent sign-in failed, display account selection prompt
            startActivityForResult(signInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
          }
        });
  }

  /**
   * Callback for items that should only be triggered after a successful sign-in of {@code
   * account}.
   */
  private void onSignInSuccess(GoogleSignInAccount account) {
    createDriveResourceClient(account);
    loadPastSums();
    enableViews();
  }

  /**
   * Builds the {@link GoogleSignInClient} that will be used to sign-in.
   */
  private GoogleSignInClient buildGoogleSignInClient() {
    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_APPFOLDER)
            .build();
    return GoogleSignIn.getClient(this, signInOptions);
  }

  /**
   * Creates the {@link DriveResourceClient} that is used in order to modify the App Data file.
   */
  private void createDriveResourceClient(GoogleSignInAccount account) {
    Log.i(TAG, "Creating DriveResourceClient.");
    mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), account);
  }

  /**
   * Submits answers to Equations by writing them to an App Data file. If an App Data file already
   * exists in {@link SharedPreferences}, it will be updated; otherwise, an App Data file is created
   * then modified.
   */
  private void submitAnswer() {
    final String driveIdStr = mSharedPreferences.getString(DRIVE_ID, null);
    if (driveIdStr != null) {
      // App Data file already exists, open it.
      DriveId fileId = DriveId.decodeFromString(driveIdStr);
      DriveFile sumFile = fileId.asDriveFile();

      mDriveResourceClient.openFile(sumFile, DriveFile.MODE_WRITE_ONLY)
          .addOnCompleteListener(new OnCompleteListener<DriveContents>() {
            @Override
            public void onComplete(@NonNull Task<DriveContents> task) {
              mSubmitButton.setEnabled(true);
            }
          })
          .addOnSuccessListener(new OnSuccessListener<DriveContents>() {
            @Override
            public void onSuccess(DriveContents driveContents) {
              updateSums(driveContents);
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.e(TAG, "Unable to retrieve contents.", e);
            }

          });
    } else {
      // App Data file does not exist yet, create it.
      mDriveResourceClient.createContents()
          .addOnCompleteListener(new OnCompleteListener<DriveContents>() {
            @Override
            public void onComplete(@NonNull Task<DriveContents> task) {
              mSubmitButton.setEnabled(true);
            }
          })
          .addOnSuccessListener(new OnSuccessListener<DriveContents>() {
            @Override
            public void onSuccess(DriveContents driveContents) {
              writeFirstSum(driveContents);
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.e(TAG, "Unable to retrieve contents.", e);
            }
          });
    }
  }

  /**
   * Deletes the App Data file associated with the {@link DriveId} in {@link SharedPreferences}, if
   * it exists.
   */
  private void deleteSavedEquations() {
    final String driveIdStr = mSharedPreferences.getString(DRIVE_ID, null);
    if (driveIdStr != null) {
      DriveId fileId = DriveId.decodeFromString(driveIdStr);

      // [START delete_file]
      // Delete App Data file
      DriveFile sumFile = fileId.asDriveFile();
      mDriveResourceClient.delete(sumFile)
          .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
              // Delete completed successfully. UI changes, etc. can now be updated to reflect the
              // change.

              // [START_EXCLUDE]
              // Remove stored DriveId from SharedPreferences
              mSharedPreferences.edit().remove(DRIVE_ID).apply();
              Log.d(TAG, "Past sums deleted.");
              // [END_EXCLUDE]
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              // Delete was not completed. Inform the user or add failure handling here.

              // [START_EXCLUDE]
              Log.e(TAG, "Unable to delete App Data.", e);
              // [END_EXCLUDE]
            }
          });
      // [END delete_file]
    }
  }
}
