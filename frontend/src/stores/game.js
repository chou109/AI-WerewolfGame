import { defineStore } from 'pinia'
import axios from 'axios'

export const useGameStore = defineStore('game', {
  state: () => ({
    currentRoom: null,
    rooms: [],
    players: [],
    gameRecords: [],
    currentPhase: '',
    currentDay: 1,
    isGameStarted: false
  }),
  getters: {
    getCurrentRoom: (state) => state.currentRoom,
    getRooms: (state) => state.rooms,
    getPlayers: (state) => state.players,
    getGameRecords: (state) => state.gameRecords,
    getCurrentPhase: (state) => state.currentPhase,
    getCurrentDay: (state) => state.currentDay,
    getIsGameStarted: (state) => state.isGameStarted
  },
  actions: {
    async fetchRooms() {
      try {
        const response = await axios.get('/game/room/available')
        if (response.data.code === 200) {
          this.rooms = response.data.data
        }
      } catch (error) {
        console.error('Fetch rooms error:', error)
      }
    },
    async createRoom(roomData) {
      try {
        const response = await axios.post('/game/room/create', roomData)
        if (response.data.code === 200) {
          this.currentRoom = response.data.data
          return true
        }
        return false
      } catch (error) {
        console.error('Create room error:', error)
        return false
      }
    },
    async getRoomInfo(roomId) {
      try {
        const response = await axios.get(`/game/room/info/${roomId}`)
        if (response.data.code === 200) {
          this.currentRoom = response.data.data
        }
      } catch (error) {
        console.error('Get room info error:', error)
      }
    },
    async getRoomById(roomId) {
      try {
        const response = await axios.get(`/game/room/info/${roomId}`)
        if (response.data.code === 200) {
          this.currentRoom = response.data.data
          return response.data.data
        }
        return null
      } catch (error) {
        console.error('Get room by id error:', error)
        return null
      }
    },
    async getPlayersByRoomId(roomId) {
      try {
        const response = await axios.get(`/game/player/list/${roomId}`)
        if (response.data.code === 200) {
          this.players = response.data.data
        }
      } catch (error) {
        console.error('Get players error:', error)
      }
    },
    async addPlayerToRoom(roomId, userId, playerNumber) {
      try {
        const response = await axios.post('/game/player/add', {
          roomId,
          userId,
          playerNumber
        })
        return response.data.code === 200
      } catch (error) {
        console.error('Add player error:', error)
        return false
      }
    },
    async leaveRoom(roomId) {
      try {
        // 这里可以根据实际需求调用后端API，或者直接返回成功
        // 例如：const response = await axios.post('/game/player/remove', { roomId, userId })
        // 暂时直接返回成功
        return true
      } catch (error) {
        console.error('Leave room error:', error)
        return false
      }
    },
    async startGame(roomId) {
      try {
        const response = await axios.put('/game/room/start', {
          roomId
        })
        if (response.data.code === 200) {
          this.isGameStarted = true
          this.currentPhase = 'night'
          this.currentDay = 1
          return true
        }
        return false
      } catch (error) {
        console.error('Start game error:', error)
        return false
      }
    },
    async getGameRecords(roomId) {
      try {
        const response = await axios.get(`/game/record/list/${roomId}`)
        if (response.data.code === 200) {
          this.gameRecords = response.data.data
        }
      } catch (error) {
        console.error('Get game records error:', error)
      }
    },
    async addGameRecord(record) {
      try {
        const response = await axios.post('/game/record/add', record)
        return response.data.code === 200
      } catch (error) {
        console.error('Add game record error:', error)
        return false
      }
    },
    setCurrentPhase(phase) {
      this.currentPhase = phase
    },
    setCurrentDay(day) {
      this.currentDay = day
    },
    resetGameState() {
      this.currentRoom = null
      this.players = []
      this.gameRecords = []
      this.currentPhase = ''
      this.currentDay = 1
      this.isGameStarted = false
    }
  }
})
