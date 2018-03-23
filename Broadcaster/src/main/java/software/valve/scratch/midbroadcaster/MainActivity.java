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

package software.valve.scratch.midbroadcaster;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.leff.midi.MidiFile;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.util.MidiProcessor;
import com.mobileer.miditools.MidiInputPortSelector;

import java.io.File;
import java.io.IOException;

/**
 * Main activity for the keyboard app.
 */
public class MainActivity extends Activity {
	private static final String TAG = "MIDBroadcaster";

	private MidiInputPortSelector mKeyboardReceiverSelector;
	private Button mProgramButton;
	private MidiManager mMidiManager;
	private int mChannel; // ranges from 0 to 15
	private EventSender ep;
	private MidiFile midi = null;
	private playStatus playstatus = playStatus.PAUSED;
	private CheckBox downCheck;
	private TextView eventLog;

	private MidiProcessor processor;

	private enum playStatus {
		PAUSED,
		PLAYING
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

		Spinner spinner = findViewById(R.id.spinner_channels);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
				int pos, long id) {
				mChannel = pos & 0x0F;
				//updateProgramText();
				if (ep != null)	ep.setBaseChannel(mChannel);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		});

		//Button testButton = (Button) findViewById(R.id.play);

		ListView midList = findViewById(R.id.midSelector);
		//populateMidiList(R.id.midSelector);
		midList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, populateMidiList()));
		midList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				try{
					midi = new MidiFile(new File(Environment.getExternalStorageDirectory() + "/MIDIs/" + adapterView.getItemAtPosition(i)));
					TextView midTitle = findViewById(R.id.midTitle);
					midTitle.setText((String) adapterView.getItemAtPosition(i));
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		});
		downCheck = findViewById(R.id.downMixChannel);
		downCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (ep != null)	ep.setDownmix(b);
			}
		});




		eventLog = findViewById(R.id.eventLog);
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

	public void playMidi(View view) {

		switch (playstatus){
			case PAUSED:
				processor = new MidiProcessor(midi);

				ep = new EventSender("MidiEvent", mKeyboardReceiverSelector, eventLog, mChannel, downCheck.isChecked() );
				processor.registerEventListener(ep, NoteOn.class);
				processor.registerEventListener(ep, NoteOff.class);
				processor.start();
				playstatus = playStatus.PLAYING;

				break;
			case PLAYING:
				processor.stop();
				playstatus = playStatus.PAUSED;
		}
	}

	@Override
	public void onDestroy() {
		ep.closeSynthResources();
		super.onDestroy();
	}

	public void setPlayButtonText(playStatus playstatus){
		int text;
		switch(playstatus){
			case PAUSED:
				text = R.string.btnPause;
				break;
			case PLAYING:
				text = R.string.btnPlay;
				break;
			default:
				text = 0;
		}
		Button button = this.findViewById(R.id.btnPlay);
		button.setText(text);
	}

	public String[] populateMidiList() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 99);
		}

		File dir = new File(Environment.getExternalStorageDirectory() + "/MIDIs/");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File[] midis = dir.listFiles();
		if (midis == null) return null;
		String[] filenames = new String[midis.length];
		int i = 0;
		for (File midi : midis) {
			filenames[i] = midi.getName();
			i++;
		}
		try {
			midi = new MidiFile(new File(Environment.getExternalStorageDirectory() + "/MIDIs/" + filenames[0]));
		} catch (IOException e) {
			System.err.println(e);
		}
		return filenames;

	}

}
