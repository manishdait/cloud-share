import { createAction, props } from "@ngrx/store";
import { File } from "../../types/file.type";

export const setFiles = createAction('[Files] Set Files', props<{files: File[]}>());
export const addFiles = createAction('[Files] Add Files', props<{files: File[]}>());
export const addFile = createAction('[Files] Add File', props<{file: File}>());
export const updateFile = createAction('[Files] Update File', props<{file: File}>());
export const deleteFile = createAction('[Files] Delete Files', props<{uuid: string}>());
