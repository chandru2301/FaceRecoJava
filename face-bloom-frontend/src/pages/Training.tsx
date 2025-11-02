import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Brain, CheckCircle, AlertCircle, Users, Code } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function Training() {
  const [method, setMethod] = useState<'auto' | 'python' | 'java'>('auto');
  const { toast } = useToast();

  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: () => api.getStudents(),
  });

  const { data: pythonStatus } = useQuery({
    queryKey: ['python-status'],
    queryFn: () => api.getPythonStatus(),
  });

  const trainMutation = useMutation({
    mutationFn: () => api.trainModel(method),
    onSuccess: (data) => {
      toast({
        title: 'Training Complete',
        description: `${data.message} using ${data.implementation}`,
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Training Failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });

  return (
    <div className="max-w-4xl space-y-8">
      <div>
        <h1 className="text-4xl font-bold bg-gradient-primary bg-clip-text text-transparent mb-2">
          Model Training
        </h1>
        <p className="text-muted-foreground">
          Train the face recognition model with registered students
        </p>
      </div>

      {/* Status Cards */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card className="gradient-card border-border/50">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Registered Students
            </CardTitle>
            <Users className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-primary">{students.length}</div>
            <p className="text-sm text-muted-foreground mt-1">
              Available for training
            </p>
          </CardContent>
        </Card>

        <Card className="gradient-card border-border/50">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Java Status
            </CardTitle>
            <Code className="h-5 w-5 text-secondary" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              {pythonStatus?.pythonAvailable ? (
                <>
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  <span className="text-lg font-medium text-green-500">Available</span>
                </>
              ) : (
                <>
                  <AlertCircle className="h-5 w-5 text-yellow-500" />
                  <span className="text-lg font-medium text-yellow-500">Not Available</span>
                </>
              )}
            </div>
           
          </CardContent>
        </Card>
      </div>

      {/* Training Configuration */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>Training Configuration</CardTitle>
          <CardDescription>
            Select the training method for face recognition
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-4">
            <Label className="text-base font-medium">Training Method</Label>
            <RadioGroup value={method} onValueChange={(v) => setMethod(v as any)}>
              <div className="flex items-center space-x-3 rounded-lg border border-border/50 p-4 hover:bg-muted/20 transition-colors">
                <RadioGroupItem value="auto" id="auto" />
                <Label htmlFor="auto" className="flex-1 cursor-pointer">
                  <div className="font-medium">Auto</div>
                  <div className="text-sm text-muted-foreground">
                    Automatically select the best available method
                  </div>
                </Label>
              </div>



              <div className="flex items-center space-x-3 rounded-lg border border-border/50 p-4 hover:bg-muted/20 transition-colors">
                <RadioGroupItem value="java" id="java" />
                <Label htmlFor="java" className="flex-1 cursor-pointer">
                  <div className="font-medium">Java</div>
                  <div className="text-sm text-muted-foreground">
                    Use Java implementation (always available)
                  </div>
                </Label>
              </div>
            </RadioGroup>
          </div>

          {students.length === 0 && (
            <div className="rounded-lg border border-yellow-500/20 bg-yellow-500/10 p-4">
              <div className="flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-yellow-500 mt-0.5" />
                <div>
                  <p className="font-medium text-yellow-500">No Students Registered</p>
                  <p className="text-sm text-yellow-500/80 mt-1">
                    Please register at least one student before training the model.
                  </p>
                </div>
              </div>
            </div>
          )}

          <Button
            onClick={() => trainMutation.mutate()}
            disabled={students.length === 0 || trainMutation.isPending}
            className="w-full bg-primary hover:bg-primary/90 glow-primary"
            size="lg"
          >
            <Brain className="mr-2 h-5 w-5" />
            {trainMutation.isPending ? 'Training Model...' : 'Train Model'}
          </Button>
        </CardContent>
      </Card>

      {/* Training Info */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>About Training</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-muted-foreground">
          <p>
            Training the model creates a mathematical representation of each registered student's face.
            This enables the system to recognize them during attendance.
          </p>
          <p>
            You should retrain the model whenever you:
          </p>
          <ul className="list-disc list-inside space-y-1 ml-2">
            <li>Register new students</li>
            <li>Delete existing students</li>
            <li>Update student photos</li>
          </ul>
          <p className="text-primary font-medium">
            Training typically takes a few seconds to complete.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
