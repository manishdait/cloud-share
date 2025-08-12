import { createReducer, on } from "@ngrx/store";
import { File } from "../../types/file.type";
import { addFile, addFiles, deleteFile, setFiles, updateFile } from "./file.action";

export interface FileState {
  files: File[];
}

export const initialFileState: FileState = {
  files: []
}

export const fileReducer = createReducer(
  initialFileState,
  on(setFiles, (state, {files}) => ({
    files: files
  })),
  on(addFiles, (state, {files}) => ({
    files: [...files, ...state.files]
  })),
  on(addFile, (state, {file}) => ({
    files: [file, ...state.files]
  })),
  on(updateFile, (state, {file}) => ({
    files: state.files.map(f => f.uuid !== file.uuid? f : file)
  })),
  on(deleteFile, (state, {uuid}) => ({
    files: state.files.filter(f => f.uuid !== uuid)
  }))
);