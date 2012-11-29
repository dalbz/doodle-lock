/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.osu.cse.doodleLock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GestureBuilderActivity extends ListActivity {
    private static final String TAG = "UserList";
    private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_CANCELLED = 1;
    private static final int STATUS_NO_STORAGE = 2;
    private static final int STATUS_NOT_LOADED = 3;

    private static final int MENU_ID_RENAME = 1;
    private static final int MENU_ID_REMOVE = 2;

    private static final int DIALOG_ADD_USER = 1;

    private static final int REQUEST_NEW_GESTURE = 1;

    // Type: long (id)
    private static final String GESTURES_INFO_ID = "gestures.info_id";

    private final File mStoreFile = new File(
            Environment.getExternalStorageDirectory(), "gestures");
    private final File mStoreDir = new File(
            Environment.getExternalStorageDirectory(), "doodlers");

    private final Comparator<? super String> mSorter = new Comparator<String>() {
        public int compare(String user1, String user2) {
            return user1.compareTo(user2);
        }
    };

    private static GestureLibrary sStore;

    private UsersAdapter mAdapter;
    private TextView mEmpty;

    private Dialog mRenameDialog;
    private EditText mInput;
    private NamedGesture mCurrentUser;
    private Map<String, File> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.users_list);

        mAdapter = new UsersAdapter(this);
        setListAdapter(mAdapter);

        mStoreDir.mkdirs();
        mEmpty = (TextView) findViewById(android.R.id.empty);
        this.loadUsers();
        this.findViewById(R.id.addButton).setEnabled(true);
        this.checkForEmpty();

        this.registerForContextMenu(getListView());
    }

    static GestureLibrary getStore() {
        return sStore;
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    public void reloadUsers(View v) {
        loadUsers();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    public void addUser(View v) {
        showDialog(DIALOG_ADD_USER);
    }

    public void startTraining(View v) {
        Intent intent = new Intent(this, CreateDoodleActivity.class);
        startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    public void calcGesture(View v) {

        // Pull all of the gestures into a more usable list
        ArrayList<Gesture> gestureList = new ArrayList<Gesture>();
        ArrayList<String> gestureNames = new ArrayList<String>();
        gestureNames.addAll(sStore.getGestureEntries());

        for (String name : gestureNames) {
            gestureList.add(sStore.getGestures(name).get(0));
        }

        Doodle currentDoodle = new Doodle(gestureList);

        for (Gesture g : gestureList) {
            boolean testResult = currentDoodle.authenticate(g);
            Log.i("INFO", "Result : " + testResult);
        }

        // Intent intent = new Intent(this, CreateDoodleActivity.class);
        // startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case REQUEST_NEW_GESTURE:
                loadUsers();
                break;
            }
        }
    }

    private void loadUsers() {
        users = new HashMap<String, File>();
        UsersAdapter adapter = mAdapter;
        adapter.clear();
        adapter.setNotifyOnChange(false);

        for (File gestureStore : mStoreDir.listFiles()) {
            String username = gestureStore.getName();
            Log.i(TAG, "Found user: " + username);
            users.put(username, gestureStore);
            adapter.add(username);
        }

        adapter.sort(mSorter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupRenameDialog();
    }

    private void checkForEmpty() {
        if (mAdapter.getCount() == 0) {
            mEmpty.setText(R.string.gestures_empty);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentUser != null) {
            outState.putLong(GESTURES_INFO_ID, mCurrentUser.gesture.getID());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        long id = state.getLong(GESTURES_INFO_ID, -1);
        if (id != -1) {
            final Set<String> entries = sStore.getGestureEntries();
            out: for (String name : entries) {
                for (Gesture gesture : sStore.getGestures(name)) {
                    if (gesture.getID() == id) {
                        mCurrentUser = new NamedGesture();
                        mCurrentUser.name = name;
                        mCurrentUser.gesture = gesture;
                        break out;
                    }
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(((TextView) info.targetView).getText());

        menu.add(0, MENU_ID_RENAME, 0, R.string.gestures_rename);
        menu.add(0, MENU_ID_REMOVE, 0, R.string.gestures_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        final File userGestures = (File) menuInfo.targetView.getTag();
        String userName = userGestures.getName();

        switch (item.getItemId()) {
        case MENU_ID_REMOVE:
            deleteUser(userName);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ADD_USER) {
            return createAddUserDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (id == DIALOG_ADD_USER) {
        }
    }

    private Dialog createAddUserDialog() {
        final View layout = View.inflate(this, R.layout.dialog_rename, null);
        mInput = (EditText) layout.findViewById(R.id.name);
        ((TextView) layout.findViewById(R.id.label))
        .setText(R.string.gestures_rename_label);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_rename_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                cleanupRenameDialog();
            }
        });
        builder.setNegativeButton("Cancel", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cleanupRenameDialog();
            }
        });
        Log.i(TAG, "herp");
        builder.setPositiveButton("Create User", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setupUser();
            }
        });
        builder.setView(layout);
        return builder.create();
    }

    private void setupUser() {
        final String name = mInput.getText().toString();
        if (!TextUtils.isEmpty(name)) {
            if (name.contains(",")) {
                Toast.makeText(getApplicationContext(),
                        "Name cannnot contain commas.", Toast.LENGTH_SHORT)
                        .show();
            } else {
                File userGestureStore = new File(mStoreDir.getPath(), name);
                try {
                    userGestureStore.createNewFile();
                } catch (IOException e) {
                }
                GestureLibrary gLib = GestureLibraries
                        .fromFile(userGestureStore);
                gLib.save();
            }
        }
        loadUsers();
        cleanupRenameDialog();
    }

    private void cleanupRenameDialog() {
        if (mRenameDialog != null) {
            mRenameDialog.dismiss();
            mRenameDialog = null;
        }
        mInput.setText("");
        mCurrentUser = null;
    }

    private void deleteUser(String user) {
        File gesturesFile = users.remove(user);
        gesturesFile.delete();

        final UsersAdapter adapter = mAdapter;
        adapter.setNotifyOnChange(false);
        adapter.remove(user);
        adapter.sort(mSorter);
        checkForEmpty();
        adapter.notifyDataSetChanged();

        Toast.makeText(this, R.string.gestures_delete_success,
                Toast.LENGTH_SHORT).show();
    }

    static class NamedGesture {
        String name;
        Gesture gesture;
    }

    private class UsersAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;

        public UsersAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.gestures_item, parent,
                        false);
            }

            final String user = getItem(position);
            final TextView label = (TextView) convertView;

            label.setTag(users.get(user));
            label.setText(user);

            return convertView;
        }
    }
}
