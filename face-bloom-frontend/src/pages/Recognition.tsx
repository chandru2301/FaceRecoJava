import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Video, Square, Play, FileSpreadsheet, CheckCircle, XCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function Recognition() {
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const { data: status } = useQuery({
    queryKey: ['recognition-status'],
    queryFn: () => api.getRecognitionStatus(),
    refetchInterval: 3000,
  });

  const { data: attendanceFile } = useQuery({
    queryKey: ['attendance-file'],
    queryFn: () => api.getAttendanceFile(),
  });

  const startMutation = useMutation({
    mutationFn: () => api.startRecognition(),
    onSuccess: (data) => {
      toast({
        title: 'Success',
        description: data.message,
      });
      queryClient.invalidateQueries({ queryKey: ['recognition-status'] });
    },
    onError: (error: Error) => {
    
    },
  });

  const stopMutation = useMutation({
    mutationFn: () => api.stopRecognition(),
    onSuccess: (data) => {
      toast({
        title: 'Success',
        description: data.message,
      });
      queryClient.invalidateQueries({ queryKey: ['recognition-status'] });
    },
    onError: (error: Error) => {
     
    },
  });

  const isRunning = status?.running || false;

  return (
    <div className="max-w-4xl space-y-8">
      <div>
        <h1 className="text-4xl font-bold bg-gradient-primary bg-clip-text text-transparent mb-2">
          Face Recognition
        </h1>
        <p className="text-muted-foreground">
          Start and monitor real-time face recognition for attendance
        </p>
      </div>

      {/* Status Card */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Recognition Status</CardTitle>
              <CardDescription>{status?.message || 'Loading...'}</CardDescription>
            </div>
            <div className={`flex items-center gap-2 ${isRunning ? 'text-green-500' : 'text-muted-foreground'}`}>
              {isRunning ? (
                <>
                  <div className="h-3 w-3 rounded-full bg-green-500 animate-pulse" />
                  <CheckCircle className="h-6 w-6" />
                </>
              ) : (
                <>
                  <div className="h-3 w-3 rounded-full bg-muted-foreground" />
                  <XCircle className="h-6 w-6" />
                </>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-center gap-4 py-8">
            <Button
              onClick={() => startMutation.mutate()}
              disabled={isRunning || startMutation.isPending}
              className="bg-green-500 hover:bg-green-600 text-white px-8"
              size="lg"
            >
              <Play className="mr-2 h-5 w-5" />
              {startMutation.isPending ? 'Starting...' : 'Start Recognition'}
            </Button>

            <Button
              onClick={() => stopMutation.mutate()}
              disabled={!isRunning || stopMutation.isPending}
              variant="destructive"
              size="lg"
              className="px-8"
            >
              <Square className="mr-2 h-5 w-5" />
              {stopMutation.isPending ? 'Stopping...' : 'Stop Recognition'}
            </Button>
          </div>

          {isRunning && (
            <div className="rounded-lg border border-green-500/20 bg-green-500/10 p-4">
              <div className="flex items-start gap-3">
                <Video className="h-5 w-5 text-green-500 mt-0.5 animate-pulse" />
                <div>
                  <p className="font-medium text-green-500">Recognition Active</p>
                  <p className="text-sm text-green-500/80 mt-1">
                    The system is monitoring the camera feed and marking attendance automatically.
                  </p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Attendance File Info */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>Attendance Records</CardTitle>
          <CardDescription>View and manage attendance data</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-lg border border-border/50 bg-muted/20 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Excel File Location</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {attendanceFile?.exists ? (
                    <>File exists at: {attendanceFile.filePath}</>
                  ) : (
                    'No attendance file generated yet'
                  )}
                </p>
              </div>
              <FileSpreadsheet className="h-8 w-8 text-primary" />
            </div>

            {attendanceFile?.exists && (
              <div className="space-y-2 text-sm text-muted-foreground">
                <p>Directory: {attendanceFile.directory}</p>
                <p>Full Path: {attendanceFile.absolutePath}</p>
                {attendanceFile.lastModified && (
                  <p>Last Modified: {new Date(attendanceFile.lastModified).toLocaleString()}</p>
                )}
                {attendanceFile.size && (
                  <p>Size: {(attendanceFile.size / 1024).toFixed(2)} KB</p>
                )}
              </div>
            )}
          </div>

          {attendanceFile?.exists && (
            <Button 
              variant="outline" 
              className="w-full"
              onClick={() => {
                // Download the file via API endpoint (browsers block file:// links)
                const downloadUrl = `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/recognition/attendance-file/download`;
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.download = 'attendance.xlsx';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
              }}
            >
              <FileSpreadsheet className="mr-2 h-4 w-4" />
              Download Attendance File
            </Button>
          )}
        </CardContent>
      </Card>

      {/* Instructions */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>How It Works</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <ol className="list-decimal list-inside space-y-2">
            <li>Click "Start Recognition" to activate the face recognition system</li>
            <li>The system will access your webcam and start monitoring for registered faces</li>
            <li>When a registered student is detected, their attendance is automatically recorded</li>
            <li>All attendance data is saved to an Excel file in the project directory</li>
            <li>Click "Stop Recognition" when you're done marking attendance</li>
          </ol>
          <p className="text-primary font-medium mt-4">
            Make sure you have trained the model with registered students before starting recognition.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
