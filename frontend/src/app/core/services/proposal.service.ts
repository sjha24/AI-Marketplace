import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/job.models';
import { Order } from '../models/order.models';
import { Proposal, SubmitProposalRequest } from '../models/proposal.models';

@Injectable({ providedIn: 'root' })
export class ProposalService {
  constructor(private readonly http: HttpClient) {}

  submit(jobId: number, request: SubmitProposalRequest): Observable<Proposal> {
    return this.http.post<Proposal>(`${environment.apiUrl}/jobs/${jobId}/proposals`, request);
  }

  listForJob(jobId: number): Observable<Proposal[]> {
    return this.http.get<Proposal[]>(`${environment.apiUrl}/jobs/${jobId}/proposals`);
  }

  getMine(page = 0, size = 20): Observable<PageResponse<Proposal>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<Proposal>>(`${environment.apiUrl}/proposals/mine`, { params });
  }

  accept(proposalId: number): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/proposals/${proposalId}/accept`, {});
  }
}
