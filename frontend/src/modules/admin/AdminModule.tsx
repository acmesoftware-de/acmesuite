import { UsersView } from './UsersView'
import { ProvidersView } from './ProvidersView'

/** Admin module content (design module 05 = ACMEbase management). Sub-view driven by the shell. */
export function AdminModule({ subView }: { subView: string }) {
  if (subView === 'authentifizierung') {
    return <ProvidersView />
  }
  return <UsersView />
}
