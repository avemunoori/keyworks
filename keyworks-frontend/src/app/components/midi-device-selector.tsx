import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface MidiDeviceSelectorProps {
  devices: Array<{id: string, name: string}>
  selectedDevice: string
  onDeviceSelect: (deviceName: string) => void
}

export default function MidiDeviceSelector({ 
  devices, 
  selectedDevice, 
  onDeviceSelect 
}: MidiDeviceSelectorProps) {
  return (
    <Select value={selectedDevice} onValueChange={onDeviceSelect}>
      <SelectTrigger className="w-full">
        <SelectValue placeholder="Select MIDI device" />
      </SelectTrigger>
      <SelectContent>
        {devices.length === 0 ? (
          <SelectItem value="none" disabled>No MIDI devices found</SelectItem>
        ) : (
          devices.map(device => (
            <SelectItem key={device.id} value={device.name}>
              {device.name}
            </SelectItem>
          ))
        )}
      </SelectContent>
    </Select>
  )
}
