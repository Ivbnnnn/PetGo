export const isAdmin = (user) => user?.accessLevel === 'ADMIN';
export const isEditor = (user) => ['EDITOR', 'ADMIN'].includes(user?.accessLevel);
export const isOwner = (user) => user?.role === 'OWNER';
export const isWalker = (user) => user?.role === 'WALKER';