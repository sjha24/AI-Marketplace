import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Category, CreateJobRequest, Job, JobListParams, JobSummary, PageResponse
} from '../models/job.models';

@Injectable({ providedIn: 'root' })
export class JobService {
  constructor(private readonly http: HttpClient) {}

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${environment.apiUrl}/categories`);
  }

  listJobs(params: JobListParams = {}): Observable<PageResponse<JobSummary>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.skillId) httpParams = httpParams.set('skillId', String(params.skillId));
    if (params.categoryId) httpParams = httpParams.set('categoryId', String(params.categoryId));
    if (params.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<PageResponse<JobSummary>>(`${environment.apiUrl}/jobs`, { params: httpParams });
  }

  getMyJobs(page = 0, size = 20): Observable<PageResponse<JobSummary>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<JobSummary>>(`${environment.apiUrl}/jobs/mine`, { params });
  }

  getJob(id: number): Observable<Job> {
    return this.http.get<Job>(`${environment.apiUrl}/jobs/${id}`);
  }

  createJob(request: CreateJobRequest): Observable<Job> {
    return this.http.post<Job>(`${environment.apiUrl}/jobs`, request);
  }

  updateJob(id: number, request: CreateJobRequest): Observable<Job> {
    return this.http.put<Job>(`${environment.apiUrl}/jobs/${id}`, request);
  }

  cancelJob(id: number): Observable<Job> {
    return this.http.post<Job>(`${environment.apiUrl}/jobs/${id}/cancel`, {});
  }
}
