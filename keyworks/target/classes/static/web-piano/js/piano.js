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
    async function handleAuthError(response) {
        if (!response.ok) {
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const errorData = await response.json();
                if (errorData.error === 'Unauthorized' || errorData.status === 401) {
                    showAuthModal();
                    return null;
                }
            } else if (response.redirected && response.url.includes('/login')) {
                // User was redirected to login page
                window.location.href = response.url;
                return null;
            } else if (response.status === 401 || response.status === 403) {
                showAuthModal();
                return null;
            }
            
            // For other errors, throw to be caught by the catch block
            throw new Error(`Request failed with status ${response.status}`);
        }
        return response;
    }
    
    function showAuthModal() {
        // Check if modal already exists
        let authModal = document.getElementById('auth-modal');
        
        if (!authModal) {
            // Create modal if it doesn't exist
            authModal = document.createElement('div');
            authModal.id = 'auth-modal';
            authModal.className = 'modal';
            authModal.innerHTML = `
                <div class="modal-content">
                    <div class="modal-header">
                        <h2>Session Expired</h2>
                        <span class="close">&times;</span>
                    </div>
                    <div class="modal-body">
                        <p>Your session has expired or you need to log in to continue.</p>
                    </div>
                    <div class="modal-footer">
                        <button id="login-button" class="btn btn-primary">Log In</button>
                        <button id="continue-guest-button" class="btn btn-secondary">Continue as Guest</button>
                    </div>
                </div>
            `;
            document.body.appendChild(authModal);
            
            // Add event listeners to modal buttons
            document.querySelector('#auth-modal .close').addEventListener('click', function() {
                authModal.style.display = 'none';
            });
            
            document.getElementById('login-button').addEventListener('click', function() {
                window.location.href = '/login';
            });
            
            document.getElementById('continue-guest-button').addEventListener('click', function() {
                authModal.style.display = 'none';
                updateStatus('Continuing as guest. Some features may be limited.', 'warning');
            });
            
            // Add modal styles if not already in the document
            if (!document.getElementById('modal-styles')) {
                const modalStyles = document.createElement('style');
                modalStyles.id = 'modal-styles';
                modalStyles.textContent = `
                    .modal {
                        display: none;
                        position: fixed;
                        z-index: 1000;
                        left: 0;
                        top: 0;
                        width: 100%;
                        height: 100%;
                        background-color: rgba(0,0,0,0.5);
                    }
                    .modal-content {
                        background-color: #fefefe;
                        margin: 15% auto;
                        padding: 20px;
                        border: 1px solid #888;
                        width: 80%;
                        max-width: 500px;
                        border-radius: 5px;
                    }
                    .modal-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 1px solid #eee;
                        padding-bottom: 10px;
                    }
                    .modal-body {
                        padding: 20px 0;
                    }
                    .modal-footer {
                        border-top: 1px solid #eee;
                        padding-top: 10px;
                        display: flex;
                        justify-content: flex-end;
                        gap: 10px;
                    }
                    .close {
                        color: #aaa;
                        font-size: 28px;
                        font-weight: bold;
                        cursor: pointer;
                    }
                    .close:hover {
                        color: black;
                    }
                `;
                document.head.appendChild(modalStyles);
            }
        }
        
        // Show the modal
        authModal.style.display = 'block';
    }
    
    /**
     * MIDI Device Functions
     */
    function loadMidiDevices() {
        fetch('/api/midi/devices')
            .then(response => handleAuthError(response))
            .then(response => {
                if (response) return response.json();
                return null;
            })
            .then(devices => {
                if (!devices) return;
                
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
            .then(response => handleAuthError(response))
            .then(response => {
                if (response) return response.json();
                return null;
            })
            .then(status => {
                if (!status) return;
                
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
                    
                    // If recording is in progress, start the timer
                    if (status.recordingStartTime) {
                        recordingStartTime = new Date(status.recordingStartTime);
                        recordingTimer = setInterval(updateRecordingTimer, 1000);
                    }
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
        
        updateStatus('Connecting...', 'info');
        
        fetch('/api/midi/connect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ deviceId: selectedDeviceId })
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
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
        updateStatus('Disconnecting...', 'info');
        
        fetch('/api/midi/disconnect', {
            method: 'POST'
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
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
        
        updateStatus('Starting recording...', 'info');
        
        fetch('/api/midi/record/start', {
            method: 'POST'
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
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
        
        updateStatus('Stopping recording...', 'info');
        
        fetch('/api/midi/record/stop', {
            method: 'POST'
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
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
                
                // Show save dialog if user is authenticated
                if (document.getElementById('user-authenticated') && data.recordingId) {
                    showSaveRecordingDialog(data.recordingId);
                } else {
                    // For guest users, show download options
                    showGuestDownloadOptions(data.recordingId);
                }
                
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
    
    function showSaveRecordingDialog(recordingId) {
        // Create modal for saving recording
        const saveModal = document.createElement('div');
        saveModal.className = 'modal';
        saveModal.id = 'save-recording-modal';
        saveModal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>Save Recording</h2>
                    <span class="close">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="recording-title">Title</label>
                        <input type="text" id="recording-title" class="form-control" placeholder="My Recording">
                    </div>
                    <div class="form-group">
                        <label for="recording-description">Description (optional)</label>
                        <textarea id="recording-description" class="form-control" rows="3"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button id="save-recording-button" class="btn btn-primary">Save</button>
                    <button id="cancel-save-button" class="btn btn-secondary">Cancel</button>
                </div>
            </div>
        `;
        document.body.appendChild(saveModal);
        
        // Show the modal
        saveModal.style.display = 'block';
        
        // Add event listeners
        document.querySelector('#save-recording-modal .close').addEventListener('click', function() {
            saveModal.remove();
        });
        
        document.getElementById('cancel-save-button').addEventListener('click', function() {
            saveModal.remove();
        });
        
        document.getElementById('save-recording-button').addEventListener('click', function() {
            const title = document.getElementById('recording-title').value || 'Untitled Recording';
            const description = document.getElementById('recording-description').value || '';
            
            saveRecording(recordingId, title, description);
            saveModal.remove();
        });
    }
    
    function saveRecording(recordingId, title, description) {
        fetch('/api/sheetmusic/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                recordingId: recordingId,
                title: title,
                description: description
            })
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
            return null;
        })
        .then(data => {
            if (data && data.success) {
                updateStatus('Recording saved successfully', 'success');
                loadUserRecordings();
            } else if (data) {
                updateStatus('Error saving recording: ' + (data.message || 'Unknown error'), 'error');
            }
        })
        .catch(error => {
            console.error('Error saving recording:', error);
            updateStatus('Error saving recording', 'error');
        });
    }
    
    function showGuestDownloadOptions(recordingId) {
        // Create modal for guest download options
        const downloadModal = document.createElement('div');
        downloadModal.className = 'modal';
        downloadModal.id = 'guest-download-modal';
        downloadModal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>Download Options</h2>
                    <span class="close">&times;</span>
                </div>
                <div class="modal-body">
                    <p>Your recording is ready! As a guest user, you can download your files directly.</p>
                    <p>To save recordings to your account, please <a href="/login">log in</a> or <a href="/register">register</a>.</p>
                    <div class="download-buttons">
                        <a href="/api/files/output/music_generated_${recordingId}.pdf" class="btn btn-primary" target="_blank">View PDF</a>
                        <a href="/api/files/output/music_generated_${recordingId}.midi" class="btn btn-secondary" download="recording.midi">Download MIDI</a>
                    </div>
                </div>
                <div class="modal-footer">
                    <button id="close-download-modal" class="btn btn-secondary">Close</button>
                </div>
            </div>
        `;
        document.body.appendChild(downloadModal);
        
        // Add some specific styles for this modal
        const modalStyle = document.createElement('style');
        modalStyle.textContent = `
            .download-buttons {
                display: flex;
                gap: 10px;
                margin: 20px 0;
            }
        `;
        document.head.appendChild(modalStyle);
        
        // Show the modal
        downloadModal.style.display = 'block';
        
        // Add event listeners
        document.querySelector('#guest-download-modal .close').addEventListener('click', function() {
            downloadModal.remove();
        });
        
        document.getElementById('close-download-modal').addEventListener('click', function() {
            downloadModal.remove();
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
            .then(response => handleAuthError(response))
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
            .then(response => handleAuthError(response))
            .then(response => {
                if (response) return response.json();
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
                    
                    const deleteBtn = document.createElement('button');
                    deleteBtn.className = 'btn btn-sm btn-danger';
                    deleteBtn.textContent = 'Delete';
                    deleteBtn.addEventListener('click', function() {
                        if (confirm('Are you sure you want to delete this recording?')) {
                            deleteRecording(recording.id);
                        }
                    });
                    
                    actions.appendChild(viewPdfBtn);
                    actions.appendChild(downloadMidiBtn);
                    actions.appendChild(deleteBtn);
                    
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
    
    function deleteRecording(recordingId) {
        fetch(`/api/sheetmusic/${recordingId}`, {
            method: 'DELETE'
        })
        .then(response => handleAuthError(response))
        .then(response => {
            if (response) return response.json();
            return null;
        })
        .then(data => {
            if (data && data.success) {
                updateStatus('Recording deleted successfully', 'success');
                loadUserRecordings();
            } else if (data) {
                updateStatus('Error deleting recording: ' + (data.message || 'Unknown error'), 'error');
            }
        })
        .catch(error => {
            console.error('Error deleting recording:', error);
            updateStatus('Error deleting recording', 'error');
        });
    }
});