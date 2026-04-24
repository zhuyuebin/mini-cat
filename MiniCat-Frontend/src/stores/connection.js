import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useConnectionStore = defineStore('connection', () => {
  const currentConnection = ref(null)
  const currentDatabase = ref(null)
  
  function setCurrentConnection(connection) {
    currentConnection.value = connection
  }
  
  function setCurrentDatabase(database) {
    currentDatabase.value = database
  }
  
  function clearCurrentConnection() {
    currentConnection.value = null
    currentDatabase.value = null
  }
  
  return {
    currentConnection,
    currentDatabase,
    setCurrentConnection,
    setCurrentDatabase,
    clearCurrentConnection
  }
})
