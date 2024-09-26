<script setup lang="ts">
import PopUp from '@/components/PopUp.vue'
import { onMounted, onUnmounted, ref } from 'vue'
import sound from "@/assets/saints.mp3"
import InfoPanel from '@/components/InfoPanel.vue'

const audio = new Audio(sound)

const playAudio = () => {
  audio.volume = .5
  audio.play()
}

const stopAudio = () => {
  audio.pause()
  audio.currentTime = 0
}

const activated = ref<boolean>(false);

const activate = () => {
  activated.value = true
  playAudio()
}

const deactivate = () => {
  stopAudio()
  codeIndex = 0
  activated.value = false
}

const secretCode = [
  'ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown',
  'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight'
]

let codeIndex = 0

const checkCode = (event: KeyboardEvent) => {
  if (activated.value) { return; }
  if (event.key === secretCode[codeIndex]) {
    codeIndex++
    if (codeIndex === secretCode.length) {
      activate()
    }
  } else {
    codeIndex = 0
  }
}

onMounted(() => {
  window.addEventListener('keydown', checkCode)
})
onUnmounted(() => {
  window.removeEventListener('keydown', checkCode);
});
</script>

<template>
  <PopUp
    v-if="activated"
    @closePopUp="deactivate"
  >
    <div id="box">
      <h1>About the CS 240 Autograder</h1>
      <InfoPanel id="about">
        <h4>The CS 240 Autograder was created from the ground up entirely by CS 240 TAs</h4>
        <p>The first commit to the Autograder GitHub Repo was December 2 2023. It was developed and first used during the Winter 2024 semester.</p>
        <p>See the mess we made <a href="https://github.com/softwareconstruction240/autograder" target="_blank">here</a>.</p>
      </InfoPanel>
      <div id="leads">
        <div class="person">
          <p class="name">Paul Hathaway <a href="https://github.com/pawlh" target="_blank"><i class="fa-solid fa-otter"/></a></p>
          <p class="title">Father of the Autograder</p>
          <p class="contributions">Wrote the foundations of the Autograder and its core grading functionality</p>
        </div>
        <div class="person">
          <p class="name">Michael Davenport <a href="https://github.com/19mdavenport" target="_blank"><i class="fa-solid fa-hat-wizard"/></a></p>
          <p class="title">Guardian of the Autograder</p>
          <p class="contributions">Developed most of the integrations with Canvas, student database handling, the code quality checker, lots of behind the scenes refactoring, and basically had a hand in every backend system in the program</p>
        </div>
      </div>

      <br/>

      <div id="team">
        <div class="person">
          <p class="name">Dallin Webecke <a href="https://webecke.dev" target="_blank"><i class="fa-solid fa-tree"/></a></p>
          <p class="contributions">Created several admin tools, cleaned up the Student UI and added Font Awesome</p>
        </div>
        <div class="person">
          <p class="name">James Finlinson <a href="https://github.com/frozenfrank" target="_blank"><i class="fa-solid fa-pen-nib"/></a></p>
          <p class="contributions">Developed class-requirement enforcement systems, like late-days and git-commits</p>
        </div>
        <div class="person">
          <p class="name">Noah Pratt <a href="https://noahpratt.com" target="_blank"><i class="fa-solid fa-ship"/></a></p>
          <p class="contributions">Developed several important admin analytical tools</p>
        </div>
        <div class="person">
          <p class="name">Isaih Barron <a href="https://github.com/Fiwafoofa" target="_blank"><i class="fa-solid fa-jet-fighter"/></a></p>
          <p class="contributions">Wrote some systems that did pre-compiling verification of student code</p>
        </div>
      </div>

    </div>
    </PopUp>
</template>

<style scoped>
* {
  text-align: center;
  text-wrap: balance;
}

#team {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.name {
  font-weight: 1000;
  margin-top: 10px;
}

#leads .name {
  font-size: 26px;
}

#team .name {
  font-size: 20px;
}

#leads .title {
  margin-top: 0;
  padding-top: 0;
  font-style: italic;
  font-size: 16px;
}

#box {
  max-width: 60vw;
}

</style>