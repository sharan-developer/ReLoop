import { api } from './api.js';

let categoriesPromise = null;
/** Categories rarely change; cache the request for the page session. */
export function getCategories() {
  if (!categoriesPromise) categoriesPromise = api.get('/api/categories').catch((e) => { categoriesPromise = null; throw e; });
  return categoriesPromise;
}
