export function liveFlag(env, app, flag) {
  const individual = env[app + '_LIVE_' + flag];
  if (individual !== undefined) return individual === 'yes';
  const list = env['URYSOFT_LIVE_' + flag + '_APPS'];
  return typeof list === 'string' && list.split(',').some(name => name.trim() === app);
}
