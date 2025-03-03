#!/bin/bash

# List all MIDI devices
curl -X GET "http://localhost:8080/api/midi/devices"

# Uncomment the line with your actual device name and comment out the others
# For Yamaha YDP-143, it might appear as one of these:

# curl -X POST "http://localhost:8080/api/midi/listen/Yamaha%20Digital%20Piano"
# curl -X POST "http://localhost:8080/api/midi/listen/YDP-143"
# curl -X POST "http://localhost:8080/api/midi/listen/USB%20MIDI%20Device"

# Wait 30 seconds to play notes
sleep 30

# Stop listening (make sure to use the same device name as above)
# curl -X POST "http://localhost:8080/api/midi/stop/Yamaha%20Digital%20Piano"
# curl -X POST "http://localhost:8080/api/midi/stop/YDP-143"
# curl -X POST "http://localhost:8080/api/midi/stop/USB%20MIDI%20Device"

# Stop all devices (this works regardless of device name)
curl -X POST "http://localhost:8080/api/midi/stop-all"