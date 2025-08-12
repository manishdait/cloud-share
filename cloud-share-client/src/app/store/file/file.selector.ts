import { createSelector } from "@ngrx/store";
import { AppState } from "../app.state";

export const selectFileState = (state: AppState) => state.fileState;

export const fileSelector = createSelector(
  selectFileState,
  (state) => state.files
);