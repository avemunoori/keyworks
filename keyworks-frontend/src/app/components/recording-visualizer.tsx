"use client"

import { useEffect, useRef } from "react"

interface Note {
  note: number
  velocity: number
  timestamp: number
  duration?: number
}

interface RecordingVisualizerProps {
  notes: Note[]
  isRecording: boolean
}

export default function RecordingVisualizer({ notes, isRecording }: RecordingVisualizerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  
  useEffect(() => {
    if (!canvasRef.current) return
    
    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    // Set canvas dimensions
    canvas.width = canvas.offsetWidth
    canvas.height = canvas.offsetHeight
    
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    
    // Draw piano roll
    drawPianoRoll(ctx, canvas.width, canvas.height)
    
    // Draw notes
    if (notes.length > 0) {
      drawNotes(ctx, notes, canvas.width, canvas.height)
    }
    
    // Draw recording indicator
    if (isRecording) {
      drawRecordingIndicator(ctx, canvas.width, canvas.height)
    }
  }, [notes, isRecording])
  
  const drawPianoRoll = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
    // Draw piano keys background
    const keyHeight = height / 88 // 88 keys on a piano
    
    for (let i = 0; i < 88; i++) {
      const isBlackKey = [1, 3, 6, 8, 10].includes(i % 12)
      ctx.fillStyle = isBlackKey ? '#333' : '#f5f5f5'
      ctx.fillRect(0, i * keyHeight, 40, keyHeight)
      ctx.strokeStyle = '#ddd'
      ctx.strokeRect(0, i * keyHeight, 40, keyHeight)
      
      // Label C keys
      if (i % 12 === 0) {
        ctx.fillStyle = '#666'
        ctx.font = '10px sans-serif'
        ctx.fillText(`C${Math.floor((i + 9) / 12)}`, 5, i * keyHeight + keyHeight - 2)
      }
    }
    
    // Draw grid lines
    ctx.strokeStyle = '#eee'
    ctx.beginPath()
    for (let i = 0; i < width; i += 50) {
      ctx.moveTo(i, 0)
      ctx.lineTo(i, height)
    }
    ctx.stroke()
  }
  
  const drawNotes = (ctx: CanvasRenderingContext2D, notes: Note[], width: number, height: number) => {
    const keyHeight = height / 88
    const timeScale = (width - 40) / (notes[notes.length - 1].timestamp + 1000) // Scale time to fit canvas
    
    notes.forEach(note => {
      // MIDI notes start at 21 (A0) and go to 108 (C8)
      const y = (108 - note.note) * keyHeight
      const x = 40 + note.timestamp * timeScale
      const noteWidth = note.duration ? note.duration * timeScale : 20
      
      // Color based on velocity (louder = more intense color)
      const intensity = Math.min(1, note.velocity / 127)
      ctx.fillStyle = `rgba(65, 105, 225, ${intensity})`
      
      // Draw note rectangle
      ctx.fillRect(x, y, noteWidth, keyHeight)
      ctx.strokeStyle = '#3366cc'
      ctx.strokeRect(x, y, noteWidth, keyHeight)
    })
  }
  
  const drawRecordingIndicator = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
    ctx.fillStyle = 'rgba(255, 0, 0, 0.5)'
    ctx.beginPath()
    ctx.arc(width - 20, 20, 10, 0, Math.PI * 2)
    ctx.fill()
  }
  
  return (
    <div className="w-full h-full min-h-[300px] border rounded-md">
      {notes.length === 0 && !isRecording ? (
        <div className="flex items-center justify-center h-full text-muted-foreground">
          No recorded notes yet
        </div>
      ) : (
        <canvas 
          ref={canvasRef} 
          className="w-full h-full"
        />
      )}
    </div>
  )
}
