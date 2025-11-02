import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Image as ImageIcon, Upload, Scan } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function RecognizeImage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [results, setResults] = useState<any>(null);
  const { toast } = useToast();

  const recognizeMutation = useMutation({
    mutationFn: (file: File) => api.recognizeImage(file),
    onSuccess: (data) => {
      setResults(data);
      toast({
        title: 'Recognition Complete',
        description: `Found ${data.faces?.length || 0} face(s)`,
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Recognition Failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setResults(null);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="max-w-4xl space-y-8">
      <div>
        <h1 className="text-4xl font-bold bg-gradient-primary bg-clip-text text-transparent mb-2">
          Image Recognition
        </h1>
        <p className="text-muted-foreground">
          Upload an image to recognize registered faces
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Upload Section */}
        <Card className="gradient-card border-border/50">
          <CardHeader>
            <CardTitle>Upload Image</CardTitle>
            <CardDescription>Select an image file to analyze</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="relative aspect-video overflow-hidden rounded-lg border-2 border-dashed border-border/50 bg-muted/20">
              {preview ? (
                <img
                  src={preview}
                  alt="Preview"
                  className="h-full w-full object-contain"
                />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <div className="text-center">
                    <ImageIcon className="mx-auto h-12 w-12 text-muted-foreground mb-2" />
                    <p className="text-sm text-muted-foreground">No image selected</p>
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="image-upload">Select Image</Label>
              <div className="flex gap-2">
                <input
                  id="image-upload"
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => document.getElementById('image-upload')?.click()}
                >
                  <Upload className="mr-2 h-4 w-4" />
                  Choose Image
                </Button>
              </div>
            </div>

            <Button
              onClick={() => selectedFile && recognizeMutation.mutate(selectedFile)}
              disabled={!selectedFile || recognizeMutation.isPending}
              className="w-full bg-primary hover:bg-primary/90 glow-primary"
            >
              <Scan className="mr-2 h-4 w-4" />
              {recognizeMutation.isPending ? 'Recognizing...' : 'Recognize Faces'}
            </Button>
          </CardContent>
        </Card>

        {/* Results Section */}
        <Card className="gradient-card border-border/50">
          <CardHeader>
            <CardTitle>Recognition Results</CardTitle>
            <CardDescription>
              {results ? `Found ${results.faces?.length || 0} face(s)` : 'No results yet'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {!results ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <Scan className="h-12 w-12 text-muted-foreground mb-4" />
                <p className="text-muted-foreground">
                  Upload and analyze an image to see results
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {results.faces && results.faces.length > 0 ? (
                  results.faces.map((face: any, index: number) => (
                    <div
                      key={index}
                      className="rounded-lg border border-border/50 bg-muted/20 p-4 space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <p className="font-medium">{face.name}</p>
                        <span className="text-sm text-primary">
                          {(face.confidence * 100).toFixed(1)}% confident
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        Department: {face.department}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        Label ID: {face.labelId}
                      </p>
                    </div>
                  ))
                ) : (
                  <div className="rounded-lg border border-yellow-500/20 bg-yellow-500/10 p-4">
                    <p className="text-sm text-yellow-500">
                      No registered faces detected in this image
                    </p>
                  </div>
                )}

                {results.implementation && (
                  <p className="text-xs text-muted-foreground text-center">
                    Recognition performed using: {results.implementation}
                  </p>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
