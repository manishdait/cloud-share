export interface File {
  readonly uuid: string;
  name: string;
  type: string;
  size: number;
  isPublic: boolean;
  uploadedAt: Date;
};