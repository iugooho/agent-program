{
  "name": "{{frontendArtifactId}}",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "description": "{{description}}",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "typecheck": "vue-tsc --noEmit",
    "test": "vitest run",
    "test:watch": "vitest",
    "coverage": "vitest run --coverage",
    "lint": "eslint . --fix",
    "format": "prettier --write \"src/**/*.{ts,vue,css}\""
  },
  "dependencies": {
{{frontendDependencies}}
  },
  "devDependencies": {
{{frontendDevDependencies}}
  }
}
