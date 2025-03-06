// API base URL
const apiBaseUrl = '/api/midi';

// DOM Elements
document.addEventListener('DOMContentLoaded', function() {
    // Device and user controls
    const deviceSelectEl = document.getElementById('deviceSelect');
    const userSelectEl = document.getElementById('userSelect');
    const refreshBtn = document.getElementById('refreshBtn');
    const refreshUsersBtn = document.getElementById('refreshUsersBtn');
    const connectBtn = document.getElementById('connectBtn');
    const disconnectBtn = document.getElementById('disconnectBtn');
    const startRecordingBtn = document.getElementById('startRecordingBtn');
    const stopRecordingBtn = document.getElementById('stopRecordingBtn');
    const downloadPdfBtn = document.getElementById('downloadPdfBtn');
    const simulateBtn = document.getElementById('simulateBtn');
    
    // Display elements
    const logContainerEl = document.getElementById('logContainer');
    const statusDisplayEl = document.getElementById('statusDisplay');
    const recordingInfoEl = document.getElementById('recordingInfo');
    const lilypondCodeEl = document.getElementById('lilypondCode');
    const noteDisplayEl = document.getElementById('noteDisplay');
    const virtualKeyboardEl = document.getElementById('virtualKeyboard');
    const tabs = document.querySelectorAll('.tab');
    const tabContents = document.querySelectorAll('.tab-content');
    
    // State
    let isConnected = false;
    let isRecording = false;
    let currentRecordingId = null;
    let recordingData = null;
    
    // Initialize the application
    function init() {
        loadDevices();
        loadUsers();
        createVirtualKeyboard();
        setupTabNavigation();
        updateUIState();
        setupEventListeners();
    }
    
    // Set up tab navigation
    function setupTabNavigation() {
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabId = tab.getAttribute('data-tab');
                
                // Update active tab
                tabs.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                
                // Update active content
                tabContents.forEach(content => content.classList.remove('active'));
                document.getElementById(tabId + 'Tab').classList.add('active');
            });
        });
    }
    
    // Set up event listeners
    function setupEventListeners() {
        if (refreshBtn) refreshBtn.addEventListener('click', loadDevices);
        if (refreshUsersBtn) refreshUsersBtn.addEventListener('click', loadUsers);
        if (connectBtn) connectBtn.addEventListener('click', connectToDevice);
        if (disconnectBtn) disconnectBtn.addEventListener('click', disconnectFromDevice);
        if (startRecordingBtn) startRecordingBtn.addEventListener('click', startRecording);
        if (stopRecordingBtn) stopRecordingBtn.addEventListener('click', stopRecording);
        if (downloadPdfBtn) downloadPdfBtn.addEventListener('click', downloadPdf);
        if (simulateBtn) simulateBtn.addEventListener('click', simulateMidiInput);
        
        if (userSelectEl) {
            userSelectEl.addEventListener('change', (e) => {
                if (e.target.value) {
                    setCurrentUser(e.target.value);
                }
            });
        }
    }
    
    // Create virtual keyboard
    function createVirtualKeyboard() {
        if (!virtualKeyboardEl) return;
        
        const octaves = 2;
        const startNote = 60; // Middle C
        
        for (let octave = 0; octave < octaves; octave++) {
            for (let note = 0; note < 12; note++) {
                const midiNote = startNote + octave * 12 + note;
                const keyEl = document.createElement('div');
                
                // Determine if it's a white or black key
                const isBlackKey = [1, 3, 6, 8, 10].includes(note);
                
                keyEl.style.width = isBlackKey ? '30px' : '40px';
                keyEl.style.height = isBlackKey ? '100px' : '150px';
                keyEl.style.backgroundColor = isBlackKey ? '#333' : 'white';
                keyEl.style.border = '1px solid #333';
                keyEl.style.marginRight = isBlackKey ? '-15px' : '2px';
                keyEl.style.zIndex = isBlackKey ? '1' : '0';
                keyEl.style.position = 'relative';
                keyEl.style.borderRadius = '0 0 4px 4px';
                keyEl.style.cursor = 'pointer';
                
                // Add note name at the bottom of white keys
                if (!isBlackKey) {
                    const noteNames = ['C', 'D', 'E', 'F', 'G', 'A', 'B'];
                    const noteName = noteNames[Math.floor(note / 12) * 7 + [0, 2, 4, 5, 7, 9, 11].indexOf(note)];
                    
                    const nameEl = document.createElement('div');
                    nameEl.textContent = noteName + (octave + 4);
                    nameEl.style.position = 'absolute';
                    nameEl.style.bottom = '5px';
                    nameEl.style.left = '0';
                    nameEl.style.right = '0';
                    nameEl.style.textAlign = 'center';
                    nameEl.style.fontSize = '12px';
                    keyEl.appendChild(nameEl);
                }
                
                // Add click event listener only
                keyEl.addEventListener('click', () => {
                    // Visual feedback
                    const originalColor = isBlackKey ? '#333' : 'white';
                    const pressedColor = isBlackKey ? '#555' : '#e6e6e6';
                    
                    keyEl.style.backgroundColor = pressedColor;
                    
                    // Send note on event
                    sendMidiEvent(true, midiNote, 100);
                    
                    // Send note off event after a short delay
                    setTimeout(() => {
                        sendMidiEvent(false, midiNote, 0);
                        keyEl.style.backgroundColor = originalColor;
                    }, 300);
                });
                
                virtualKeyboardEl.appendChild(keyEl);
            }
        }
    }
    
    // Send MIDI event from virtual keyboard
    async function sendMidiEvent(isNoteOn, note, velocity) {
        try {
            const response = await fetch(`${apiBaseUrl}/event?isNoteOn=${isNoteOn}&note=${note}&velocity=${velocity}`, {
                method: 'POST'
            });
            
            const data = await response.json();
            log(`MIDI Event: ${data.message}`);
        } catch (error) {
            log(`Error sending MIDI event: ${error.message}`);
        }
    }
    
    // Load available MIDI devices
    async function loadDevices() {
        try {
            const response = await fetch(`${apiBaseUrl}/devices`);
            const devices = await response.json();
            
            // Clear existing options
            deviceSelectEl.innerHTML = '<option value="">-- Select MIDI Device --</option>';
            
            // Add device options
            devices.forEach(device => {
                const option = document.createElement('option');
                option.value = device;
                option.textContent = device;
                deviceSelectEl.appendChild(option);
            });
            
            log(`Loaded ${devices.length} MIDI devices`);
        } catch (error) {
            log(`Error loading devices: ${error.message}`);
        }
    }
    
    // Load users
    async function loadUsers() {
        if (!userSelectEl) return;
        
        try {
            const response = await fetch('/api/users');
            const users = await response.json();
            
            // Clear existing options
            userSelectEl.innerHTML = '<option value="">-- Select User --</option>';
            
            // Add user options
            users.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.username;
                userSelectEl.appendChild(option);
            });
            
            log(`Loaded ${users.length} users`);
        } catch (error) {
            log(`Error loading users: ${error.message}`);
        }
    }
    
    // Set current user
    async function setCurrentUser(userId) {
        try {
            const response = await fetch(`${apiBaseUrl}/set-user?userId=${userId}`, {
                method: 'POST'
            });
            const data = await response.json();
            log(`Set current user: ${data.message}`);
        } catch (error) {
            log(`Error setting user: ${error.message}`);
        }
    }
    
    // Connect to selected MIDI device
    async function connectToDevice() {
        const deviceName = deviceSelectEl.value;
        
        if (!deviceName) {
            log('Please select a MIDI device');
            return;
        }
        
        try {
            const response = await fetch(`${apiBaseUrl}/device/connect?deviceName=${encodeURIComponent(deviceName)}`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                isConnected = true;
                log(`Connected to device: ${deviceName}`);
                updateUIState();
            } else {
                log(`Failed to connect: ${data.message}`);
            }
        } catch (error) {
            log(`Error connecting to device: ${error.message}`);
        }
    }
    
    // Disconnect from MIDI device
    async function disconnectFromDevice() {
        const deviceName = deviceSelectEl.value;
        
        if (!deviceName) {
            log('No device selected');
            return;
        }
        
        try {
            const response = await fetch(`${apiBaseUrl}/device/disconnect?deviceName=${encodeURIComponent(deviceName)}`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                isConnected = false;
                log(`Disconnected from device: ${deviceName}`);
                updateUIState();
            } else {
                log(`Failed to disconnect: ${data.message}`);
            }
        } catch (error) {
            log(`Error disconnecting from device: ${error.message}`);
        }
    }
    
    // Start recording
    async function startRecording() {
        const deviceName = deviceSelectEl.value;
        
        if (!deviceName) {
            log('Please select a MIDI device');
            return;
        }
        
        try {
            const response = await fetch(`${apiBaseUrl}/record/start?deviceName=${encodeURIComponent(deviceName)}`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                isRecording = true;
                currentRecordingId = data.recordingId;
                log(`Started recording with ID: ${currentRecordingId}`);
                updateUIState();
            } else {
                log(`Failed to start recording: ${data.message}`);
            }
        } catch (error) {
            log(`Error starting recording: ${error.message}`);
        }
    }
    
    // Stop recording
    async function stopRecording() {
        try {
            const response = await fetch(`${apiBaseUrl}/record/stop`, {
                method: 'POST'
            });
            
            recordingData = await response.json();
            isRecording = false;
            
            log(`Stopped recording. Captured ${recordingData.noteCount} notes`);
            updateUIState();
            updateRecordingInfo();
        } catch (error) {
            log(`Error stopping recording: ${error.message}`);
        }
    }
    
    // Download PDF
    async function downloadPdf() {
        if (!currentRecordingId) {
            log('No recording available to download');
            return;
        }
        
        try {
            log(`Downloading PDF for recording: ${currentRecordingId}`);
            
            // Get the selected user ID
            const userId = userSelectEl ? userSelectEl.value : null;
            
            // Create a direct link to download the PDF
            let downloadUrl = `${window.location.origin}/api/sheet-music/from-recording/${currentRecordingId}`;
            
            // Add user ID if selected
            if (userId) {
                downloadUrl += `?userId=${userId}`;
            }
            
            // Create a temporary link element and click it to trigger the download
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.target = '_blank';
            link.download = `recording-${currentRecordingId}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            log('PDF download initiated');
        } catch (error) {
            log(`Error downloading PDF: ${error.message}`);
        }
    }
    
    // Simulate MIDI input
    async function simulateMidiInput() {
        try {
            // Simulate a C major scale
            const notes = [60, 62, 64, 65, 67, 69, 71, 72];
            
            const response = await fetch(`${apiBaseUrl}/simulate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(notes)
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                currentRecordingId = data.recordingId;
                log(`Simulated MIDI input with recording ID: ${currentRecordingId}`);
                
                // Fetch the recording data
                const recordingResponse = await fetch(`${apiBaseUrl}/recording/${currentRecordingId}`);
                recordingData = await recordingResponse.json();
                
                updateRecordingInfo();
                updateUIState();
            } else {
                log(`Failed to simulate MIDI input: ${data.message}`);
            }
        } catch (error) {
            log(`Error simulating MIDI input: ${error.message}`);
        }
    }
    
    // Update recording info display
    function updateRecordingInfo() {
        if (!recordingData) {
            if (recordingInfoEl) recordingInfoEl.innerHTML = '<p>No active recording.</p>';
            if (lilypondCodeEl) lilypondCodeEl.textContent = '% No LilyPond code generated yet';
            if (noteDisplayEl) noteDisplayEl.innerHTML = '<p>No notes recorded yet.</p>';
            return;
        }
        
        // Update recording info tab
        if (recordingInfoEl) {
            recordingInfoEl.innerHTML = `
                <h3>Recording ID: ${recordingData.id}</h3>
                <p>Notes: ${recordingData.noteCount}</p>
                <p>Duration: ${Math.round(recordingData.duration / 1000)} seconds</p>
                ${recordingData.sheetMusicId ? `<p>Sheet Music ID: ${recordingData.sheetMusicId}</p>` : ''}
                ${recordingData.userId ? `<p>User ID: ${recordingData.userId}</p>` : ''}
            `;
        }
        
        // Update LilyPond code tab
        if (lilypondCodeEl) {
            lilypondCodeEl.textContent = recordingData.lilyPondCode || '% No LilyPond code generated';
        }
        
        // Update notes tab
        if (noteDisplayEl) {
            if (recordingData.notes && recordingData.notes.length > 0) {
                noteDisplayEl.innerHTML = '';
                recordingData.notes.forEach(note => {
                    const noteEl = document.createElement('div');
                    noteEl.className = 'note';
                    noteEl.textContent = `Note ${note.key} (${midiNoteToName(note.key)}) - Velocity: ${note.velocity} - Duration: ${Math.round(note.duration)}ms`;
                    noteDisplayEl.appendChild(noteEl);
                });
            } else {
                noteDisplayEl.innerHTML = '<p>No notes recorded.</p>';
            }
        }
    }
    
    // Convert MIDI note number to note name
    function midiNoteToName(midiNote) {
        const noteNames = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
        const octave = Math.floor(midiNote / 12) - 1;
        const noteName = noteNames[midiNote % 12];
        return `${noteName}${octave}`;
    }
    
    // Update UI state based on connection and recording status
    function updateUIState() {
        // Update button states
        if (connectBtn) connectBtn.disabled = isConnected;
        if (disconnectBtn) disconnectBtn.disabled = !isConnected;
        if (startRecordingBtn) startRecordingBtn.disabled = !isConnected || isRecording;
        if (stopRecordingBtn) stopRecordingBtn.disabled = !isRecording;
        if (downloadPdfBtn) downloadPdfBtn.disabled = !currentRecordingId;
        
        // Update status display
        if (statusDisplayEl) {
            let statusClass = 'status-disconnected';
            let statusText = 'Disconnected';
            
            if (isConnected) {
                statusClass = isRecording ? 'status-recording' : 'status-connected';
                statusText = isRecording ? 'Recording' : 'Connected';
            }
            
            statusDisplayEl.innerHTML = `<p><span class="status-indicator ${statusClass}"></span> Status: ${statusText}</p>`;
        }
    }
    
    // Log a message to the log container
    function log(message) {
        if (!logContainerEl) {
            console.log(message);
            return;
        }
        
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        
        const time = new Date().toLocaleTimeString();
        logEntry.innerHTML = `<span class="log-time">[${time}]</span> ${message}`;
        
        logContainerEl.appendChild(logEntry);
        logContainerEl.scrollTop = logContainerEl.scrollHeight;
    }
    
    // CSRF token handling for secure requests
    function getCsrfToken() {
        const tokenEl = document.querySelector('meta[name="_csrf"]');
        return tokenEl ? tokenEl.getAttribute('content') : null;
    }
    
    function getCsrfHeader() {
        const headerEl = document.querySelector('meta[name="_csrf_header"]');
        return headerEl ? headerEl.getAttribute('content') : 'X-CSRF-TOKEN';
    }
    
    // Initialize when DOM is loaded
    init();
});