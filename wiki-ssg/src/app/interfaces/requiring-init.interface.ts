export interface RequiringInit {
  init(): Promise<void>
}
