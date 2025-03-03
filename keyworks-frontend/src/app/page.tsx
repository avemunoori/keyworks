"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Mic, StopCircle, Download, Play, RefreshCw } from 'lucide-react'
import MidiDeviceSelector from "@/app/components/midi-device-selector"
import RecordingVisualizer from "@/app/components/recording-visualizer"
import SheetMusicViewer from "@/app/components/sheet-music-viewer"

// Define types for our data structures
interface MidiNote {
  note: number
  velocity: number
  timestamp: number
  duration?: number
}

interface MidiDevice {
  id: string
  name: string
}

interface SheetMusic {
  id: number
  title: string
  lilypondCode: string
  pdfUrl: string
}

export default function Home() {
  const [isRecording, setIsRecording] = useState(false)
  const [recordingDuration, setRecordingDuration] = useState(0)
  const [selectedDevice, setSelectedDevice] = useState("")
  const [recordedNotes, setRecordedNotes] = useState<MidiNote[]>([])
  const [generatedSheetMusic, setGeneratedSheetMusic] = useState<SheetMusic | null>(null)
  const [availableDevices, setAvailableDevices] = useState<MidiDevice[]>([])
  
  // Fetch available MIDI devices on component mount
  useEffect(() => {
    fetchMidiDevices()
  }, [])
  
  // Timer for recording duration
  useEffect(() => {
    let interval: NodeJS.Timeout | undefined
    if (isRecording) {
      interval = setInterval(() => {
        setRecordingDuration(prev => prev + 1)
      }, 1000)
    } else {
      setRecordingDuration(0)
    }
    return () => {
      if (interval) clearInterval(interval)
    }
  }, [isRecording])
  
  const fetchMidiDevices = async () => {
    try {
      const response = await fetch('/api/midi/devices')
      const data = await response.json()
      setAvailableDevices(data)
    } catch (error) {
      console.error("Failed to fetch MIDI devices:", error)
    }
  }
  
  const startRecording = async () => {
    if (!selectedDevice) return
    
    try {
      await fetch(`/api/midi/record/start?deviceName=${encodeURIComponent(selectedDevice)}`, {
        method: 'POST'
      })
      setIsRecording(true)
      setRecordedNotes([])
    } catch (error) {
      console.error("Failed to start recording:", error)
    }
  }
  
  const stopRecording = async () => {
    try {
      const response = await fetch('/api/midi/record/stop', {
        method: 'POST'
      })
      const data = await response.json()
      setRecordedNotes(data.notes || [])
      setIsRecording(false)
      
      // Generate sheet music from recorded notes
      generateSheetMusic(data.recordingId)
    } catch (error) {
      console.error("Failed to stop recording:", error)
    }
  }
  
  const generateSheetMusic = async (recordingId: string) => {
    try {
      const response = await fetch(`/api/sheetmusic/generate?recordingId=${recordingId}`)
      const data = await response.json()
      setGeneratedSheetMusic(data)
    } catch (error) {
      console.error("Failed to generate sheet music:", error)
    }
  }
  
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  
  return (
    <div className="container mx-auto py-8">
      <h1 className="text-3xl font-bold mb-6">KeyWorks MIDI Recorder</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-1">
          <CardHeader>
            <CardTitle>MIDI Device</CardTitle>
            <CardDescription>Select your MIDI input device</CardDescription>
          </CardHeader>
          <CardContent>
            <MidiDeviceSelector 
              devices={availableDevices}
              selectedDevice={selectedDevice}
              onDeviceSelect={setSelectedDevice}
            />
            <Button 
              variant="outline" 
              size="sm" 
              className="mt-2" 
              onClick={fetchMidiDevices}
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh Devices
            </Button>
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button 
              onClick={startRecording} 
              disabled={isRecording || !selectedDevice}
              className="bg-red-500 hover:bg-red-600"
            >
              <Mic className="h-4 w-4 mr-2" />
              Record
            </Button>
            <Button 
              onClick={stopRecording} 
              disabled={!isRecording}
              variant="outline"
            >
              <StopCircle className="h-4 w-4 mr-2" />
              Stop
            </Button>
          </CardFooter>
        </Card>
        
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>
              {isRecording ? `Recording... ${formatTime(recordingDuration)}` : 'Ready to Record'}
            </CardTitle>
            <CardDescription>
              {isRecording 
                ? 'Play your MIDI keyboard to record notes' 
                : 'Press Record to start capturing MIDI input'}
            </CardDescription>
          </CardHeader>
          <CardContent className="min-h-[300px]">
            <RecordingVisualizer 
              notes={recordedNotes} 
              isRecording={isRecording} 
            />
          </CardContent>
        </Card>
      </div>
      
      {generatedSheetMusic && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Generated Sheet Music</CardTitle>
            <CardDescription>
              View and download your sheet music
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="preview">
              <TabsList>
                <TabsTrigger value="preview">Preview</TabsTrigger>
                <TabsTrigger value="lilypond">LilyPond Code</TabsTrigger>
              </TabsList>
              <TabsContent value="preview" className="min-h-[400px]">
                <SheetMusicViewer pdfUrl={generatedSheetMusic.pdfUrl} />
              </TabsContent>
              <TabsContent value="lilypond">
                <pre className="bg-muted p-4 rounded-md overflow-auto max-h-[400px]">
                  {generatedSheetMusic.lilypondCode}
                </pre>
              </TabsContent>
            </Tabs>
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button variant="outline">
              <Play className="h-4 w-4 mr-2" />
              Play MIDI
            </Button>
            <Button>
              <Download className="h-4 w-4 mr-2" />
              Download PDF
            </Button>
          </CardFooter>
        </Card>
      )}
    </div>
  )
}