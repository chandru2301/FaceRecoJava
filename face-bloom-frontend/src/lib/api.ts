const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export interface Student {
  id: number;
  name: string;
  department: string;
  imagePath: string;
  labelId: number;
  registeredAt: string;
}

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
}

export interface RecognitionStatus {
  running: boolean;
  message: string;
}

export interface TrainingResponse {
  success: boolean;
  message: string;
  trained_count?: number;
  implementation?: 'java' | 'python';
}

export interface PythonStatus {
  pythonAvailable: boolean;
  message: string;
}

export interface AttendanceFile {
  filePath: string;
  exists: boolean;
  absolutePath: string;
  directory: string;
  size?: number;
  lastModified?: string;
}

class Api {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  // Student endpoints
  async registerStudent(name: string, department: string, image: File) {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('department', department);
    formData.append('image', image);

    const response = await fetch(`${this.baseUrl}/api/students/register`, {
      method: 'POST',
      body: formData,
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Failed to register student');
    }

    return data;
  }

  async getStudents(): Promise<Student[]> {
    const response = await fetch(`${this.baseUrl}/api/students`);
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch students' }));
      throw new Error(error.message || 'Failed to fetch students');
    }
    return response.json();
  }

  async deleteStudent(id: number): Promise<ApiResponse> {
    const response = await fetch(`${this.baseUrl}/api/students/${id}`, {
      method: 'DELETE',
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Failed to delete student');
    }
    return data;
  }

  // Training endpoints
  async trainModel(usePython: 'auto' | 'python' | 'java' = 'auto'): Promise<TrainingResponse> {
    const response = await fetch(`${this.baseUrl}/api/training/train?usePython=${usePython}`, {
      method: 'POST',
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Training failed');
    }
    return response.json();
  }

  async getPythonStatus(): Promise<PythonStatus> {
    const response = await fetch(`${this.baseUrl}/api/training/python-status`);
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to check Python status' }));
      throw new Error(error.message || 'Failed to check Python status');
    }
    return response.json();
  }

  // Recognition endpoints
  async startRecognition(): Promise<ApiResponse> {
    const response = await fetch(`${this.baseUrl}/api/recognition/start`, {
      method: 'POST',
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'Failed to start recognition');
    return data;
  }

  async stopRecognition(): Promise<ApiResponse> {
    const response = await fetch(`${this.baseUrl}/api/recognition/stop`, {
      method: 'POST',
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Failed to stop recognition');
    }
    return data;
  }

  async getRecognitionStatus(): Promise<RecognitionStatus> {
    const response = await fetch(`${this.baseUrl}/api/recognition/status`);
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch recognition status' }));
      throw new Error(error.message || 'Failed to fetch recognition status');
    }
    return response.json();
  }

  async getAttendanceFile(): Promise<AttendanceFile> {
    const response = await fetch(`${this.baseUrl}/api/recognition/attendance-file`);
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch attendance file info' }));
      throw new Error(error.message || 'Failed to fetch attendance file info');
    }
    return response.json();
  }

  async recognizeImage(image: File, usePython?: string) {
    const formData = new FormData();
    formData.append('image', image);
    if (usePython) formData.append('usePython', usePython);

    const response = await fetch(`${this.baseUrl}/api/recognition/recognize-image`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Recognition failed');
    }

    return response.json();
  }
}

export const api = new Api(API_BASE_URL);
