package software.valve.scratch.midbroadcaster;

import android.media.midi.MidiReceiver;
import android.widget.TextView;

import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.util.MidiEventListener;
import com.mobileer.miditools.MidiConstants;
import com.mobileer.miditools.MidiInputPortSelector;

import java.io.IOException;

public class EventSender implements MidiEventListener {
	private String mLabel;
	private int[] mPrograms = new int[MidiConstants.MAX_CHANNELS]; // ranges from 0 to 127
	private MidiInputPortSelector mKeyboardReceiverSelector;
	private byte[] mByteBuffer = new byte[3];
	private int baseChannel = 0;
	private TextView eventLog;

	public void setDownmix(boolean downmix) {
		this.downmix = downmix;
	}

	private boolean downmix = false;

	public EventSender(String label, MidiInputPortSelector midiInputPortSelector, TextView logger, int baseChannel, boolean downmix) {
		mLabel = label;
		mKeyboardReceiverSelector = midiInputPortSelector;
		this.baseChannel = baseChannel;
		this.downmix = downmix;
		eventLog = logger;
	}

	// 0. Implement the listener functions that will be called by the
	// MidiProcessor
	@Override
	public void onStart(boolean fromBeginning) {
		if (fromBeginning) {
			System.out.println(mLabel + " Started!");
		} else {
			System.out.println(mLabel + " resumed");
		}
	}

	@Override
	public void onEvent(MidiEvent event, long ms) {
		//System.out.println(mLabel + " received event: " + event);
		if (event instanceof NoteOn) {
			NoteOn midi = (NoteOn) event;
			if (downmix) midi.setChannel(1);
			//eventLog.setText(midi.getTick() + "#" + midi.getNoteValue());
			noteOn(midi.getChannel() + baseChannel, midi.getNoteValue(), midi.getVelocity());

		}
		if (event instanceof NoteOff) {
			NoteOff midi = (NoteOff) event;
			if (downmix) midi.setChannel(1);
			noteOff(midi.getChannel() + baseChannel, midi.getNoteValue(), midi.getVelocity());
		}
	}

	@Override
	public void onStop(boolean finished) {
		if (finished) {
			System.out.println(mLabel + " Finished!");
		} else {
			System.out.println(mLabel + " paused");
		}
	}

	public static void main(String[] args) {

	}

	private void noteOff(int channel, int pitch, int velocity) {
		midiCommand(MidiConstants.STATUS_NOTE_OFF + channel, pitch, velocity);
	}

	private void noteOn(int channel, int pitch, int velocity) {
		midiCommand(MidiConstants.STATUS_NOTE_ON + channel, pitch, velocity);
	}

	private void midiCommand(int status, int data1, int data2) {
		mByteBuffer[0] = (byte) status;
		mByteBuffer[1] = (byte) data1;
		mByteBuffer[2] = (byte) data2;
		long now = System.nanoTime();
		midiSend(mByteBuffer, 3, now);
	}

	private void midiCommand(int status, int data1) {
		mByteBuffer[0] = (byte) status;
		mByteBuffer[1] = (byte) data1;
		long now = System.nanoTime();
		midiSend(mByteBuffer, 2, now);
	}

	public void closeSynthResources() {
		if (mKeyboardReceiverSelector != null) {
			mKeyboardReceiverSelector.close();
			mKeyboardReceiverSelector.onDestroy();
		}
	}


	private void midiSend(byte[] buffer, int count, long timestamp) {
		if (mKeyboardReceiverSelector != null) {
			try {
				// send event immediately
				MidiReceiver receiver = mKeyboardReceiverSelector.getReceiver();
				if (receiver != null) {
					receiver.send(buffer, 0, count, timestamp);
				}
			} catch (IOException e) {
				//Log.e(TAG, "mKeyboardReceiverSelector.send() failed " + e);
			}
		}
	}

	public void setBaseChannel(int baseChannel) {
		this.baseChannel = baseChannel;
	}
}
