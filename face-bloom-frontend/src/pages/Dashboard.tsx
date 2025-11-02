import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Users, Brain, Video, CheckCircle, AlertCircle, FileSpreadsheet } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: () => api.getStudents(),
  });

  const { data: status } = useQuery({
    queryKey: ['recognition-status'],
    queryFn: () => api.getRecognitionStatus(),
    refetchInterval: 3000,
  });

  const stats = [
    {
      title: 'Total Students',
      value: students.length,
      icon: Users,
      color: 'text-primary',
    },
    {
      title: 'Recognition Status',
      value: status?.running ? 'Active' : 'Stopped',
      icon: Video,
      color: status?.running ? 'text-green-500' : 'text-muted-foreground',
    },
    {
      title: 'System Status',
      value: 'Online',
      icon: CheckCircle,
      color: 'text-green-500',
    },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-bold bg-gradient-primary bg-clip-text text-transparent mb-2">
          Face Recognition Attendance
        </h1>
        <p className="text-muted-foreground">
          Monitor and manage your attendance system
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-6 md:grid-cols-3">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.title} className="gradient-card border-border/50">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.title}
                </CardTitle>
                <Icon className={`h-5 w-5 ${stat.color}`} />
              </CardHeader>
              <CardContent>
                <div className={`text-3xl font-bold ${stat.color}`}>
                  {stat.value}
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Quick Actions */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>Manage your attendance system</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Link to="/register">
            <Button className="w-full bg-primary hover:bg-primary/90 text-primary-foreground glow-primary">
              <Users className="mr-2 h-4 w-4" />
              Register Student
            </Button>
          </Link>
          <Link to="/training">
            <Button variant="secondary" className="w-full">
              <Brain className="mr-2 h-4 w-4" />
              Train Model
            </Button>
          </Link>
          <Link to="/recognition">
            <Button variant="secondary" className="w-full">
              <Video className="mr-2 h-4 w-4" />
              {status?.running ? 'Stop' : 'Start'} Recognition
            </Button>
          </Link>
          <Link to="/students">
            <Button variant="outline" className="w-full">
              <FileSpreadsheet className="mr-2 h-4 w-4" />
              View Students
            </Button>
          </Link>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card className="gradient-card border-border/50">
        <CardHeader>
          <CardTitle>System Information</CardTitle>
          <CardDescription>Current status and configuration</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <div className={`h-3 w-3 rounded-full ${status?.running ? 'bg-green-500 animate-pulse' : 'bg-muted-foreground'}`} />
              <div>
                <p className="font-medium">Recognition System</p>
                <p className="text-sm text-muted-foreground">{status?.message || 'Ready to start'}</p>
              </div>
            </div>
            <Link to="/recognition">
              <Button size="sm" variant="ghost">
                Manage
              </Button>
            </Link>
          </div>

          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <Users className="h-5 w-5 text-primary" />
              <div>
                <p className="font-medium">Registered Students</p>
                <p className="text-sm text-muted-foreground">{students.length} students in database</p>
              </div>
            </div>
            <Link to="/students">
              <Button size="sm" variant="ghost">
                View All
              </Button>
            </Link>
          </div>

          {status?.running && (
            <div className="flex items-center gap-2 rounded-lg border border-green-500/20 bg-green-500/10 p-4">
              <AlertCircle className="h-5 w-5 text-green-500" />
              <p className="text-sm">Recognition is currently active and monitoring for attendance</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
