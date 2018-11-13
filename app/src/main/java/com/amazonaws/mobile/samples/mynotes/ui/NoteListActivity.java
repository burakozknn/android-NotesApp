/*
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazonaws.mobile.samples.mynotes.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.samples.mynotes.Injection;
import com.amazonaws.mobile.samples.mynotes.NotesApp;
import com.amazonaws.mobile.samples.mynotes.R;
import com.amazonaws.mobile.samples.mynotes.models.Note;
import com.amazonaws.mobile.samples.mynotes.services.AnalyticsService;
import com.amazonaws.mobile.samples.mynotes.viewmodels.NoteListViewModel;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent;

import java.util.HashMap;

public class NoteListActivity extends AppCompatActivity {
    /**
     * If the device is running in two-pane mode, then this is set to true.  In two-pane mode,
     * the UI is a side-by-side, with the list on the left and the details on the right.  In one
     * pane mode, the list and details are separate pages.
     */
    private boolean twoPane = false;

    /**
     * The view model
     */
    private NoteListViewModel viewModel;

    /**
     * The analytics service
     */
    private AnalyticsService analyticsService = Injection.getAnalyticsService();

    public static PinpointManager pinpointManager;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(this).get(NoteListViewModel.class);
        setContentView(R.layout.activity_note_list);
        if (findViewById(R.id.note_detail_container) != null) twoPane = true;

        // Configure the action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());

        // Add an item click handler to the floating action button for adding a note
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((View v) -> loadNoteDetailFragment("new"));

        // Create the adapter that will be used to load items into the recycler view
        final NoteListAdapter adapter = new NoteListAdapter((Note item) -> loadNoteDetailFragment(item.getNoteId()));

        // Create the swipe-to-delete handler
        SwipeToDelete swipeHandler = new SwipeToDelete(this, (Note item) -> viewModel.removeNote(item.getNoteId()));
        ItemTouchHelper swipeToDelete = new ItemTouchHelper(swipeHandler);

        // Configure the note list
        RecyclerView note_list = findViewById(R.id.note_list);
        swipeToDelete.attachToRecyclerView(note_list);
        note_list.setAdapter(adapter);

        // Ensure the note list is updated whenever the repository is updated
        viewModel.getNotesList().observe(this, adapter::submitList);

        // Initialize the AWS Mobile Client
        AWSMobileClient.getInstance().initialize(this).execute();

        PinpointConfiguration config = new PinpointConfiguration(
                NoteListActivity.this,
                AWSMobileClient.getInstance().getCredentialsProvider(),
                AWSMobileClient.getInstance().getConfiguration()
        );

        pinpointManager = new PinpointManager(config);
        pinpointManager.getSessionClient().startSession();
        pinpointManager.getAnalyticsClient().submitEvents();

        logEvent();
    }

    // (Optional) Create a function and customize the event recording.

    public void logEvent() {
        final AnalyticsEvent event =
                pinpointManager.getAnalyticsClient().createEvent("EventName")
                        .withAttribute("DemoAttribute1", "DemoAttributeValue1")
                        .withAttribute("DemoAttribute2", "DemoAttributeValue2")
                        .withMetric("DemoMetric1", Math.random());

        pinpointManager.getAnalyticsClient().recordEvent(event);
        pinpointManager.getAnalyticsClient().submitEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        HashMap<String,String> attributes = new HashMap<>();
        attributes.put("twoPane", twoPane ? "true" : "false");
        analyticsService.recordEvent("NoteListActivity", attributes, null);
    }

    /**
     * Loads the note details the right way, depending on if this is two-pane mode.
     *
     * @param noteId the ID of the note to load
     */
    private void loadNoteDetailFragment(String noteId) {
        if (twoPane) {
            Fragment fragment = new NoteDetailFragment();
            Bundle arguments = new Bundle();
            arguments.putString(NotesApp.ITEM_ID, noteId);
            fragment.setArguments(arguments);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.note_detail_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, NoteDetailActivity.class);
            intent.putExtra(NotesApp.ITEM_ID, noteId);
            startActivity(intent);
        }
    }
}
