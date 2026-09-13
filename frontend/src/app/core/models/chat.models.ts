export type MessageType = 'TEXT' | 'SYSTEM' | 'FILE';

export interface ChatMessage {
  id: number;
  conversationId: number;
  orderId: number;
  senderId: number | null;
  senderDisplayName: string;
  messageType: MessageType;
  body: string;
  attachmentUrl: string | null;
  createdAt: string;
  mine: boolean;
  readByMe: boolean;
}

export interface ParticipantPresence {
  userId: number;
  displayName: string;
  online: boolean;
}

export interface Conversation {
  id: number;
  orderId: number;
  createdAt: string;
  unreadCount: number;
  participants: ParticipantPresence[];
}

export interface PresenceResponse {
  orderId: number;
  participants: ParticipantPresence[];
}
