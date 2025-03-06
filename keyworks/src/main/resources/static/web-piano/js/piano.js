/**
 * KeyWorks Piano JavaScript
 * Handles MIDI device connection, recording, and virtual piano keyboard
 */

document.addEventListener('DOMContentLoaded', function() {
    // DOM Elements
    const pianoContainer = document.getElementById('piano-container');
    const deviceSelect = document.getElementById('device-select');
    const connectButton = document.getElementById('connect-button');
    const recordButton = document.getElementById('record-button');
    const stopButton = document.getElementById('stop-button');
    const statusElement = document.getElementById('status');
    const recordingsList = document.getElementById('recordings-list');
    
    // State variables
    let isConnected = false;
    let isRecording = false;
    let selectedDeviceId = null;
    let recordingStartTime = null;
    let recordingTimer = null;
    let recordingDuration = 0;
    const recordingTimerElement = document.getElementById('recording-timer');
    
    // Create piano keyboard
    createPianoKeyboard();
    
    // Load MIDI devices on page load
    loadMidiDevices();
    
    // Check MIDI status on page load
    checkMidiStatus();
    
    // Load user recordings if authenticated
    if (document.getElementById('user-authenticated')) {
        loadUserRecordings();
    }
    
    // Event listeners
    deviceSelect.addEventListener('change', function() {
        selectedDeviceId = this.value;
        connectButton.disabled = !selectedDeviceId;
    });
    
    connectButton.addEventListener('click', function() {
        if (isConnected) {
            disconnectMidiDevice();
        } else {
            connectMidiDevice();
        }
    });
    
    recordButton.addEventListener('click', function() {
        startRecording();
    });
    
    stopButton.addEventListener('click', function() {
        stopRecording();
    });
    
    /**
     * Authentication handling
     */
    function handleAuthError(response) {
        if (response.redirected && response.url.includes('/login')) {
            // User was redirected to login page
            window.location.href = response.url;
            return null;
        }
        return response;
    }
    
    /**
     * MIDI Device Functions
     */
    function loadMidiDevices() {
        fetch('/api/midi/devices')
            .then(response => response.json())
            .then(devices => {
                deviceSelect.innerHTML = '<option value="">Select a MIDI device...</option>';
                
                devices.forEach(device => {
                    const option = document.createElement('option');
                    option.value = device.id;
                    option.textContent = device.name;
                    deviceSelect.appendChild(option);
                });
                
                // Enable/disable connect button based on selection
                connectButton.disabled = !deviceSelect.value;
            })
            .catch(error => {
                console.error('Error loading MIDI devices:', error);
                updateStatus('Error loading MIDI devices', 'error');
            });
    }
    
    function checkMidiStatus() {
        fetch('/api/midi/status')
            .then(response => response.json())
            .then(status => {
                isConnected = status.connected;
                isRecording = status.recording;
                
                if (isConnected) {
                    // Find and select the connected device in the dropdown
                    const options = Array.from(deviceSelect.options);
                    const connectedOption = options.find(option => option.textContent.includes(status.currentDevice));
                    if (connectedOption) {
                        deviceSelect.value = connectedOption.value;
                        selectedDeviceId = connectedOption.value;
                    }
                    
                    updateConnectButton(true);
                    updateStatus('Connected to ' + status.currentDevice, 'success');
                    recordButton.disabled = false;
                } else {
                    updateConnectButton(false);
                    updateStatus('Not connected', 'info');
                    recordButton.disabled = true;
                }
                
                if (isRecording) {
                    updateRecordingUI(true);
                } else {
                    updateRecordingUI(false);
                }
            })
            .catch(error => {
                console.error('Error checking MIDI status:', error);
                updateStatus('Error checking MIDI status', 'error');
            });
    }
    
    function connectMidiDevice() {
        if (!selectedDeviceId) {
            updateStatus('Please select a MIDI device', 'warning');
            return;
        }
        
        fetch('/api/midi/connect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ deviceId: selectedDeviceId })
        })
        .then(handleAuthError)
        .then(response => {
            if (response) {
                return response.json();
            }
            return null;
        })
        .then(data => {
            if (data) {
                isConnected = true;
                updateConnectButton(true);
                updateStatus('Connected to ' + data.deviceName, 'success');
                recordButton.disabled = false;
            }
        })
        .catch(error => {
            console.error('Error connecting to MIDI device:', error);
            updateStatus('Error connecting to MIDI device', 'error');
        });
    }
    
    function disconnectMidiDevice() {
        fetch('/api/midi/disconnect', {
            method: 'POST'
        })
        .then(handleAuthError)
        .then(response => {
            if (response) {
                return response.json();
            }
            return null;
        })
        .then(data => {
            if (data) {
                isConnected = false;
                updateConnectButton(false);
                updateStatus('Disconnected', 'info');
                recordButton.disabled = true;
                
                if (isRecording) {
                    stopRecording();
                }
            }
        })
        .catch(error => {
            console.error('Error disconnecting MIDI device:', error);
            updateStatus('Error disconnecting MIDI device', 'error');
        });
    }
    
    /**
     * Recording Functions
     */
    function startRecording() {
        if (!isConnected) {
            updateStatus('Please connect to a MIDI device first', 'warning');
            return;
        }
        
        fetch('/api/midi/record/start', {
            method: 'POST'
        })
        .then(handleAuthError)
        .then(response => {
            if (response) {
                return response.json();
            }
            return null;
        })
        .then(data => {
            if (data) {
                isRecording = true;
                recordingStartTime = new Date();
                updateRecordingUI(true);
                updateStatus('Recording started', 'success');
                
                // Start recording timer
                recordingTimer = setInterval(updateRecordingTimer, 1000);
            }
        })
        .catch(error => {
            console.error('Error starting recording:', error);
            updateStatus('Error starting recording', 'error');
        });
    }
    
    function stopRecording() {
        if (!isRecording) {
            return;
        }
        
        fetch('/api/midi/record/stop', {
            method: 'POST'
        })
        .then(handleAuthError)
        .then(response => {
            if (response) {
                return response.json();
            }
            return null;
        })
        .then(data => {
            if (data) {
                isRecording = false;
                updateRecordingUI(false);
                updateStatus('Recording stopped', 'success');
                
                // Stop recording timer
                clearInterval(recordingTimer);
                recordingDuration = 0;
                recordingTimerElement.textContent = '00:00';
                
                // Reload recordings list if authenticated
                if (document.getElementById('user-authenticated')) {
                    loadUserRecordings();
                }
            }
        })
        .catch(error => {
            console.error('Error stopping recording:', error);
            updateStatus('Error stopping recording', 'error');
        });
    }
    
    function updateRecordingTimer() {
        if (!recordingStartTime) return;
        
        const now = new Date();
        recordingDuration = Math.floor((now - recordingStartTime) / 1000);
        
        const minutes = Math.floor(recordingDuration / 60).toString().padStart(2, '0');
        const seconds = (recordingDuration % 60).toString().padStart(2, '0');
        
        recordingTimerElement.textContent = `${minutes}:${seconds}`;
    }
    
    /**
     * UI Update Functions
     */
    function updateConnectButton(connected) {
        if (connected) {
            connectButton.textContent = 'Disconnect';
            connectButton.classList.remove('btn-primary');
            connectButton.classList.add('btn-danger');
            deviceSelect.disabled = true;
        } else {
            connectButton.textContent = 'Connect';
            connectButton.classList.remove('btn-danger');
            connectButton.classList.add('btn-primary');
            deviceSelect.disabled = false;
        }
    }
    
    function updateRecordingUI(recording) {
        if (recording) {
            recordButton.disabled = true;
            stopButton.disabled = false;
            recordingTimerElement.parentElement.classList.remove('d-none');
        } else {
            recordButton.disabled = !isConnected;
            stopButton.disabled = true;
            recordingTimerElement.parentElement.classList.add('d-none');
        }
    }
    
    function updateStatus(message, type) {
        statusElement.textContent = message;
        statusElement.className = '';
        statusElement.classList.add('status', `status-${type}`);
    }
    
    /**
     * Piano Keyboard Functions
     */
    function createPianoKeyboard() {
        const octaves = 2;
        const startOctave = 4; // Middle C (C4)
        const notes = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
        
        // Clear existing piano keys
        pianoContainer.innerHTML = '';
        
        // Create piano keyboard
        for (let octave = startOctave; octave < startOctave + octaves; octave++) {
            const octaveContainer = document.createElement('div');
            octaveContainer.className = 'octave';
            
            for (let i = 0; i < notes.length; i++) {
                const note = notes[i];
                const isBlack = note.includes('#');
                
                const key = document.createElement('div');
                key.className = isBlack ? 'key black' : 'key white';
                key.dataset.note = note + octave;
                
                // Add event listeners for mouse interaction
                key.addEventListener('mousedown', function() {
                    playNote(this.dataset.note);
                    this.classList.add('active');
                });
                
                key.addEventListener('mouseup', function() {
                    this.classList.remove('active');
                });
                
                key.addEventListener('mouseleave', function() {
                    this.classList.remove('active');
                });
                
                // Add label to white keys
                if (!isBlack) {
                    const label = document.createElement('span');
                    label.className = 'key-label';
                    label.textContent = note + octave;
                    key.appendChild(label);
                }
                
                octaveContainer.appendChild(key);
            }
            
            pianoContainer.appendChild(octaveContainer);
        }
    }
    
    function playNote(note) {
        // If connected to a MIDI device, send note to server
        if (isConnected) {
            fetch('/api/midi/play', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ note: note })
            })
            .then(handleAuthError)
            .catch(error => {
                console.error('Error playing note:', error);
            });
        }
        
        // Also play note using Web Audio API for immediate feedback
        playNoteLocally(note);
    }
    
    // Simple Web Audio API synthesizer for local playback
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    
    function playNoteLocally(note) {
        const noteFrequencies = {
            'C4': 261.63, 'C#4': 277.18, 'D4': 293.66, 'D#4': 311.13, 'E4': 329.63, 'F4': 349.23,
            'F#4': 369.99, 'G4': 392.00, 'G#4': 415.30, 'A4': 440.00, 'A#4': 466.16, 'B4': 493.88,
            'C5': 523.25, 'C#5': 554.37, 'D5': 587.33, 'D#5': 622.25, 'E5': 659.25, 'F5': 698.46,
            'F#5': 739.99, 'G5': 783.99, 'G#5': 830.61, 'A5': 880.00, 'A#5': 932.33, 'B5': 987.77
        };
        
        const frequency = noteFrequencies[note];
        if (!frequency) return;
        
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.type = 'sine';
        oscillator.frequency.value = frequency;
        
        gainNode.gain.setValueAtTime(0.5, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 1);
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.start();
        oscillator.stop(audioContext.currentTime + 1);
    }
    
    /**
     * User Recordings Functions
     */
    function loadUserRecordings() {
        fetch('/api/sheetmusic')
            .then(handleAuthError)
            .then(response => {
                if (response) {
                    return response.json();
                }
                return null;
            })
            .then(recordings => {
                if (!recordings) return;
                
                recordingsList.innerHTML = '';
                
                if (recordings.length === 0) {
                    recordingsList.innerHTML = '<p>No recordings yet.</p>';
                    return;
                }
                
                recordings.forEach(recording => {
                    const item = document.createElement('div');
                    item.className = 'recording-item';
                    
                    const title = document.createElement('h4');
                    title.textContent = recording.title;
                    
                    const description = document.createElement('p');
                    description.textContent = recording.description || 'No description';
                    
                    const date = document.createElement('p');
                    date.className = 'recording-date';
                    date.textContent = 'Created: ' + new Date(recording.createdAt).toLocaleString();
                    
                    const actions = document.createElement('div');
                    actions.className = 'recording-actions';
                    
                    const viewPdfBtn = document.createElement('a');
                    viewPdfBtn.className = 'btn btn-sm btn-primary';
                    viewPdfBtn.textContent = 'View PDF';
                    viewPdfBtn.href = `/api/sheetmusic/${recording.id}/pdf`;
                    viewPdfBtn.target = '_blank';
                    
                    const downloadMidiBtn = document.createElement('a');
                    downloadMidiBtn.className = 'btn btn-sm btn-secondary';
                    downloadMidiBtn.textContent = 'Download MIDI';
                    downloadMidiBtn.href = `/api/sheetmusic/${recording.id}/midi`;
                    downloadMidiBtn.download = `recording-${recording.id}.midi`;
                    
                    actions.appendChild(viewPdfBtn);
                    actions.appendChild(downloadMidiBtn);
                    
                    item.appendChild(title);
                    item.appendChild(description);
                    item.appendChild(date);
                    item.appendChild(actions);
                    
                    recordingsList.appendChild(item);
                });
            })
            .catch(error => {
                console.error('Error loading recordings:', error);
                updateStatus('Error loading recordings', 'error');
            });
    }
});