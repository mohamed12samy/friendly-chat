/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    public static final int RC_SIGN_IN =1;
    private static final int RC_PHOTO_PICKER =  2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    //firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMesseageDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        mFirebaseDatabase = FirebaseDatabase.getInstance(); /*main point to get access to our database*/ //GETINSTANCE instead of new (getting existing obj rather than creating a new one singleton pattern)
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mMesseageDatabaseReference = mFirebaseDatabase.getReference().child("messages"); //binding specific location
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos"); //binding specific location

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                //here we use startActivityForResult instead of startActivity because here ther is an returning value (result) which is the photo and will use it in onActivityResult
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { //disabling and enabling the send button under some conditions
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click

                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);

                mMesseageDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });
        //moved it beacause it is in oncreat
       /* mChildEventListener = new ChildEventListener() {
            @Override *//*called when any messages get into database*//*
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { //DataSnapshot object contains data from our database in firebase
                FriendlyMessage f = dataSnapshot.getValue(FriendlyMessage.class); //this class must have the exact fields that in database
                mMessageAdapter.add(f);
            }

            @Override *//*when update happens on content of messages*//*
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override *//*when a message was removed*//*
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override *//*when any message change the posittion in the list*//*
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override *//*when error happens when trying to do some changes (typically you don't have a premesion to read data)*//*
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mMesseageDatabaseReference.addChildEventListener(mChildEventListener); *//*identify the path that will go to ( messages ) and if any other path has changes in it this will not work*//*
*/
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override                               //called when it's add to FirebaseAuth object at onResume
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth){
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){//user is signed in
                    onSignedInInitialize(user.getDisplayName());
                    Toast.makeText(MainActivity.this,"you're here",Toast.LENGTH_LONG).show();
                }
                else{ //user is signed out so display sign in forms
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setTheme(R.style.FirebaseLoginTheme)
                                    .setLogo(R.drawable.logo)
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build() )).build(),RC_SIGN_IN);
                }
            }
        };
    }

    private void onSignedInInitialize(String Username){

        mUsername = Username;
        attachDatabaseReadListener();
    }
    private void onSignedOutCleanUp(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        DeattachDatabaseReadListener();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){ // if it's an log in requestcode code
            if(resultCode == RESULT_OK){
                //signed in
                Toast.makeText(MainActivity.this,"signed in ",Toast.LENGTH_SHORT).show();
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this,"CANCELED",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Toast.makeText(MainActivity.this , "PHOTOSELECTED" ,Toast.LENGTH_SHORT).show();
            Uri selectedImageUri = data.getData();
            final StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot/*the key to getting thr\e URL of the file that was just sent to the storage*/) {
                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri dlUri = uri;
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, dlUri.toString());
                            mMesseageDatabaseReference.push().setValue(friendlyMessage);
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener); //ensure authentication when resuming the app
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener); //removing authentication when pausing the app
        }
        DeattachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    private void attachDatabaseReadListener(){
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override /*called when any messages get into database*/
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { //DataSnapshot object contains data from our database in firebase
                    FriendlyMessage f = dataSnapshot.getValue(FriendlyMessage.class); //this class must have the exact fields that in database
                    mMessageAdapter.add(f);
                }

                @Override /*when update happens on content of messages*/
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
                @Override /*when a message was removed*/
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
                @Override /*when any message change the posittion in the list*/
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
                @Override /*when error happens when trying to do some changes (typically you don't have a premesion to read data)*/
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            mMesseageDatabaseReference.addChildEventListener(mChildEventListener); /*identify the path that will go to ( messages ) and if any other path has changes in it this will not work*/
        }
    }

    private void DeattachDatabaseReadListener(){

        if(mChildEventListener != null) {
            mMesseageDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

}
