import { useState, useRef, useCallback, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Camera, UserPlus, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function Register() {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [department, setDepartment] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const startCamera = useCallback(async () => {
    try {
      // Check if getUserMedia is supported
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        toast({
          title: 'Camera Error',
          description: 'Camera access is not supported in this browser.',
          variant: 'destructive',
        });
        return;
      }

      setIsLoading(true);
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { 
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user'
        },
        audio: false,
      });
      
      setStream(mediaStream);
      // The useEffect will handle setting srcObject and listening for metadata
    } catch (error: any) {
      setIsLoading(false);
      let errorMsg = 'Error accessing camera: ';
      if (error.name === 'NotAllowedError') {
        errorMsg += 'Permission denied. Please allow camera access.';
      } else if (error.name === 'NotFoundError') {
        errorMsg += 'No camera found. Please connect a camera.';
      } else {
        errorMsg += error.message;
      }
      toast({
        title: 'Camera Error',
        description: errorMsg,
        variant: 'destructive',
      });
    }
  }, [toast]);

  // Auto-start camera on mount
  useEffect(() => {
    startCamera();
    return () => {
      // Cleanup on unmount
      stopCamera();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update video element when stream changes
  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream;
      
      // Listen for video metadata load - same approach as HTML version
      const handleLoadedMetadata = () => {
        if (videoRef.current) {
          console.log('Video ready. Dimensions:', videoRef.current.videoWidth, 'x', videoRef.current.videoHeight);
          setIsLoading(false);
          toast({
            title: 'Camera Started',
            description: 'Please position your face in the frame.',
          });
        }
      };
      
      videoRef.current.addEventListener('loadedmetadata', handleLoadedMetadata, { once: true });
      
      return () => {
        if (videoRef.current) {
          videoRef.current.removeEventListener('loadedmetadata', handleLoadedMetadata);
        }
        if (stream) {
          stream.getTracks().forEach((track) => track.stop());
        }
      };
    }
  }, [stream, toast]);

  const stopCamera = useCallback(() => {
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      setStream(null);
    }
  }, [stream]);

  const capturePhoto = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) {
      toast({
        title: 'Error',
        description: 'Please wait for camera to be ready.',
        variant: 'destructive',
      });
      return;
    }

    // Check if video is ready - same check as HTML version
    if (!stream || videoRef.current.readyState !== videoRef.current.HAVE_ENOUGH_DATA) {
      toast({
        title: 'Error',
        description: 'Please wait for camera to be ready.',
        variant: 'destructive',
      });
      return;
    }

    const context = canvasRef.current.getContext('2d');
    if (!context || videoRef.current.videoWidth === 0 || videoRef.current.videoHeight === 0) {
      toast({
        title: 'Error',
        description: 'Video dimensions are not available. Please try again.',
        variant: 'destructive',
      });
      return;
    }

    // Set canvas dimensions to match video - same as HTML version
    canvasRef.current.width = videoRef.current.videoWidth;
    canvasRef.current.height = videoRef.current.videoHeight;
    
    // Draw the video frame - no mirroring (like HTML version)
    context.drawImage(videoRef.current, 0, 0);
    
    // Convert to blob first (like HTML version) then to data URL
    canvasRef.current.toBlob((blob) => {
      if (!blob) {
        toast({
          title: 'Error',
          description: 'Failed to capture image. Please try again.',
          variant: 'destructive',
        });
        return;
      }
      
      // Validate blob size (should be at least 1KB) - same as HTML version
      if (blob.size < 1024) {
        toast({
          title: 'Error',
          description: 'Captured image is too small. Please try again.',
          variant: 'destructive',
        });
        return;
      }

      // Convert blob to data URL for preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setCapturedImage(reader.result as string);
        stopCamera();
        toast({
          title: 'Photo Captured',
          description: 'Review and submit when ready.',
        });
      };
      reader.readAsDataURL(blob);
    }, 'image/jpeg', 0.95);
  }, [stream, stopCamera, toast]);

  const registerMutation = useMutation({
    mutationFn: async () => {
      if (!capturedImage || !name || !department) {
        throw new Error('Please fill all fields and capture a photo');
      }

      // Convert data URL to blob (same approach as HTML version)
      const response = await fetch(capturedImage);
      const blob = await response.blob();
      
      // Validate blob size - same as HTML version
      if (blob.size === 0) {
        throw new Error('Captured image is invalid. Please capture again.');
      }

      const file = new File([blob], 'face.jpg', { type: 'image/jpeg' });

      return api.registerStudent(name, department, file);
    },
    onSuccess: () => {
      toast({
        title: 'Success',
        description: 'Student registered successfully!',
      });
      queryClient.invalidateQueries({ queryKey: ['students'] });
      setName('');
      setDepartment('');
      setCapturedImage(null);
      // Restart camera after successful registration
      startCamera();
    },
    onError: (error: Error) => {
      toast({
        title: 'Registration Failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  return (
    <div className="max-w-4xl space-y-8">
      <div>
        <h1 className="text-4xl font-bold bg-gradient-primary bg-clip-text text-transparent mb-2">
          Register New Student
        </h1>
        <p className="text-muted-foreground">
          Capture a photo and fill in student details
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Camera Section */}
        <Card className="gradient-card border-border/50">
          <CardHeader>
            <CardTitle>Capture Photo</CardTitle>
            <CardDescription>Use your webcam to take a student photo</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="relative aspect-video overflow-hidden rounded-lg border border-border/50 bg-black">
              {!capturedImage && stream && (
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className="h-full w-full object-cover"
                />
              )}
              {capturedImage && (
                <img
                  src={capturedImage}
                  alt="Captured"
                  className="h-full w-full object-cover"
                />
              )}
              {(!stream && !capturedImage && !isLoading) && (
                <div className="flex h-full items-center justify-center flex-col">
                  <Camera className="h-16 w-16 text-muted-foreground mb-4" />
                  <p className="text-sm text-muted-foreground">Camera not started</p>
                </div>
              )}
              {isLoading && (
                <div className="flex h-full items-center justify-center flex-col">
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mb-4" />
                  <p className="text-sm text-muted-foreground">Starting camera...</p>
                </div>
              )}
            </div>

            <canvas ref={canvasRef} className="hidden" />

            <div className="flex gap-2">
              {!stream && !capturedImage && (
                <Button onClick={startCamera} className="flex-1 bg-primary hover:bg-primary/90 glow-primary">
                  <Camera className="mr-2 h-4 w-4" />
                  Start Camera
                </Button>
              )}
              {stream && !capturedImage && (
                <>
                  <Button onClick={capturePhoto} className="flex-1 bg-primary hover:bg-primary/90 glow-primary">
                    <Camera className="mr-2 h-4 w-4" />
                    Capture Photo
                  </Button>
                  <Button onClick={stopCamera} variant="outline">
                    <X className="h-4 w-4" />
                  </Button>
                </>
              )}
              {capturedImage && (
                <Button
                  onClick={() => {
                    setCapturedImage(null);
                    startCamera();
                  }}
                  variant="outline"
                  className="flex-1"
                >
                  Retake Photo
                </Button>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Form Section */}
        <Card className="gradient-card border-border/50">
          <CardHeader>
            <CardTitle>Student Details</CardTitle>
            <CardDescription>Enter student information</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name *</Label>
              <Input
                id="name"
                placeholder="Enter student name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="bg-background/50"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="department">Department *</Label>
              <Input
                id="department"
                placeholder="Enter department"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                className="bg-background/50"
              />
            </div>

            <div className="rounded-lg border border-border/50 bg-muted/20 p-4">
              <h4 className="mb-2 font-medium">Photo Status</h4>
              <p className="text-sm text-muted-foreground">
                {capturedImage
                  ? '✓ Photo captured successfully'
                  : 'No photo captured yet'}
              </p>
            </div>

            <Button
              onClick={() => registerMutation.mutate()}
              disabled={!capturedImage || !name || !department || registerMutation.isPending}
              className="w-full bg-primary hover:bg-primary/90 glow-primary"
            >
              <UserPlus className="mr-2 h-4 w-4" />
              {registerMutation.isPending ? 'Registering...' : 'Register Student'}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
