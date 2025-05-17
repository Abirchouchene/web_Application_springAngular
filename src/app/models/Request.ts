import { CategoryRequest } from "./CategoryRequest";
import { Priority } from "./Priority";
import { Question } from "./Question";
import { RequestType } from "./RequestType";
import { Status } from "./Status";
import { User } from "./User";
import { Contact } from "./Contact";

export interface Request {
  idR: number;
  userId: number;
  requestType: RequestType;
  status: Status;
  description: string;
  priority: Priority;
  categoryRequest: CategoryRequest;
  contactIds: number[];
  contacts?: Contact[];
  note?: string;
  questions?: Question[];
  questionIds?: number[];
  newQuestions?: string[];
  user: User;
  agent?: User;
  attachmentPath?: string;
  assignedAgentId?: number;
  deadline?: Date;
  createdAt?: Date;
  updatedAt?: Date;
}
