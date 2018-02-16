/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.mobileer.midikeyboard;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.leff.midi.examples.EventPrinter;
import com.leff.midi.examples.EventSender;
import com.mobileer.miditools.MidiConstants;
import com.mobileer.miditools.MidiEventScheduler;
import com.mobileer.miditools.MidiInputPortSelector;
import com.mobileer.miditools.MusicKeyboardView;

import com.leff.midi.MidiFile;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.util.MidiEventListener;
import com.leff.midi.util.MidiProcessor;


import java.io.File;
import java.io.IOException;
import java.util.EventListener;

/**
 * Main activity for the keyboard app.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MidiKeyboard";

    private MidiInputPortSelector mKeyboardReceiverSelector;
    //private MusicKeyboardView mKeyboard;
    private Button mProgramButton;
    private MidiManager mMidiManager;
    private int mChannel; // ranges from 0 to 15
    private EventSender ep;


    public class ChannelSpinnerActivity implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            mChannel = pos & 0x0F;
            //updateProgramText();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(this, "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinner_channels);
        spinner.setOnItemSelectedListener(new ChannelSpinnerActivity());

        //Button testButton = (Button) findViewById(R.id.test_button);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 99);
        }
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Setup Spinner that selects a MIDI input port.
        mKeyboardReceiverSelector = new MidiInputPortSelector(mMidiManager,
                this, R.id.spinner_receivers);

    }

    public void beginTest(View view){
        MidiFile midi = null;
        try{
            midi = new MidiFile(new File(Environment.getExternalStorageDirectory() + "/inputmid.mid"));
        } catch(IOException e){
            System.err.println(e);
            return;
        }
        MidiProcessor processor = new MidiProcessor(midi);

        ep = new EventSender("MidiEvent", mKeyboardReceiverSelector);
        processor.registerEventListener(ep, NoteOn.class);

        processor.start();
    }

    @Override
    public void onDestroy() {
        ep.closeSynthResources();
        super.onDestroy();
    }



}
