export type Plan = 'BASIC' | 'PREMIUM' | 'ULTIMATE';

export const Plans: {[key in Plan]: {amount:number, credits: number, recommended: boolean}} = {
  'BASIC': {
    amount: 100,
    credits: 100,
    recommended: false
  },
  'PREMIUM': {
    amount: 500,
    credits: 500,
    recommended: false
  },
  'ULTIMATE': {
    amount: 2500,
    credits: 5000,
    recommended: true
  }
}