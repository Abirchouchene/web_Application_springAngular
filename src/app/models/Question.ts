export interface Question {
    id: number;
    question: string;
    questionType: 'YES_NO' | 'NUMBER' | 'TEXT'; 
    responses?: Response[]; 
    response?: string; 
  }
  