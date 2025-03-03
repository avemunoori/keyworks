"use client"

import { useState, useEffect } from "react"
import { Skeleton } from "@/components/ui/skeleton"

interface SheetMusicViewerProps {
  pdfUrl: string
}

export default function SheetMusicViewer({ pdfUrl }: SheetMusicViewerProps) {
  const [loading, setLoading] = useState(true)
  
  useEffect(() => {
    // Simulate PDF loading
    const timer = setTimeout(() => {
      setLoading(false)
    }, 1500)
    
    return () => clearTimeout(timer)
  }, [])
  
  if (loading) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-[400px] w-full" />
      </div>
    )
  }
  
  return (
    <div className="w-full h-[400px] border rounded-md overflow-hidden">
      <iframe 
        src={`${pdfUrl}#toolbar=0&navpanes=0`} 
        className="w-full h-full"
        title="Sheet Music PDF"
      />
    </div>
  )
}
