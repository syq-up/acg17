<template>
  <el-dropdown trigger="click">
    <button class="btn-upload">
      <span class="btn-upload-content">上传作品<icon icon="#icon-down"></icon></span>
    </button>
    <button class="btn-upload-mobile">
      <icon icon="#icon-upload"></icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="item in catalogList" :key="'upload-' + item.index" @click="showDrawer(item.index)">
          <div class="item">
            <icon :icon="item.icon"></icon> {{ item.title }}
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <!-- 上传对话框 -->
  <el-dialog v-model="drawer.show" :title="getDialogTitle()" width="1000px" :close-on-click-modal="true"
    class="upload-dialog">
    <div class="dialog-content">
      <!-- 插画上传 -->
      <template v-if="drawer.active === 1">
        <div class="upload-section">
          <el-upload class="upload-area" drag multiple :show-file-list="false" name="file" :action="illustration.action"
            :accept="illustration.accept" :before-upload="beforeUpload" :http-request="customUpload">
            <div class="upload-content">
              <div class="upload-icon">
                <icon icon="#icon-upload"></icon>
              </div>
              <div class="upload-text">
                <p class="primary-text">拖拽图片到此处上传</p>
                <p class="secondary-text">或 <em>点击选择文件</em></p>
                <p class="hint-text">支持 JPG、PNG、GIF 格式，单个文件不超过 100MB</p>
              </div>
            </div>
          </el-upload>

          <div class="uploaded-images" v-if="illustration.list.length > 0">
            <h4>已上传的图片</h4>
            <div class="image-grid">
              <div class="image-item" v-for="item in illustration.list" :key="item.id">
                <img :src="item.url" alt="" />
                <div class="image-overlay">
                  <button class="remove-btn" @click="removeImage(item.id)">
                    <icon icon="#icon-close"></icon>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 小说上传 -->
      <template v-if="drawer.active === 4">
        <div class="novel-section">
          <div class="tab-header">
            <button class="tab-btn" :class="{ active: novel.show }" @click="switchToNovel">
              <icon icon="#icon-book"></icon>
              新增书籍
            </button>
            <button class="tab-btn" :class="{ active: chapter.show }" @click="switchToChapter">
              <icon icon="#icon-novel"></icon>
              新增章节
            </button>
          </div>

          <!-- 新增书籍表单 -->
          <form v-if="novel.show" class="modern-form" @submit="addNovel">
            <div class="form-row">
              <div class="form-group">
                <label for="novel-title" class="form-label">
                  <span class="required">*</span>书名
                </label>
                <input id="novel-title" v-model="novel.title" class="form-input" placeholder="请输入书名" autocomplete="off"
                  maxlength="100" required />
              </div>
              <div class="form-group">
                <label for="novel-author" class="form-label">作者</label>
                <input id="novel-author" v-model="novel.author" class="form-input" placeholder="请输入作者名" autocomplete="off"
                  maxlength="100" />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">标签</label>
              <div class="tag-selector">
                <!-- 新增标签按钮/输入框 -->
                <div class="tag-item add-tag-btn" v-if="!novel.addingTag" @click="startAddTag">
                  <icon icon="#icon-quill-pen" class="add-icon"></icon>
                </div>
                <div class="tag-item add-tag-input" v-else>
                  <input ref="newTagInput" v-model="novel.newTagName" class="new-tag-input" placeholder="输入标签名" maxlength="32"
                    @keyup.enter="confirmAddTag" @keyup.esc="cancelAddTag" />
                  <icon icon="#icon-ok" class="confirm-icon" @click="confirmAddTag"></icon>
                  <icon icon="#icon-close" class="cancel-icon" @click="cancelAddTag"></icon>
                </div>

                <!-- 现有标签 -->
                <div class="tag-item" v-for="tag in novel.tagList" :key="'upload-tag-' + tag.id"
                  :class="{ active: novel.tags.includes(tag.name) }" @click="toggleTag(tag.name)">
                  {{ tag.name }}
                </div>
              </div>
            </div>

            <div class="form-actions">
              <span class="error-message" v-if="novel.error">{{ novel.error }}</span>
              <button type="submit" class="submit-btn">
                <icon icon="#icon-upload"></icon>
                新增书籍
              </button>
            </div>
          </form>

          <!-- 新增章节表单 -->
          <form v-if="chapter.show" class="modern-form" @submit="addChapter">
            <div class="form-group">
              <label class="form-label">
                <span class="required">*</span>书名
              </label>
              <input v-model="chapter.novelTitle" class="form-input" autocomplete="off" readonly disabled />
            </div>

            <div class="form-group">
              <label for="chapter-title" class="form-label">
                <span class="required">*</span>章节名
              </label>
              <input id="chapter-title" v-model="chapter.title" class="form-input" placeholder="请输入章节名" autocomplete="off"
                maxlength="150" required />
            </div>

            <div class="form-group">
              <label for="chapter-content" class="form-label">
                <span class="required">*</span>章节内容
              </label>
              <textarea id="chapter-content" v-model="chapter.content" class="form-textarea" placeholder="请输入章节内容"
                rows="8" autocomplete="off" required></textarea>
            </div>

            <div class="form-actions">
              <span class="error-message" v-if="chapter.error">{{ chapter.error }}</span>
              <button type="submit" class="submit-btn">
                <icon icon="#icon-upload"></icon>
                新增章节
              </button>
            </div>
          </form>
        </div>
      </template>

      <!-- 漫画上传 -->
      <template v-if="drawer.active === 2">
        <div class="manga-section">
          <div class="tab-header">
            <button class="tab-btn" :class="{ active: manga.show }" @click="switchToManga">
              <icon icon="#icon-manga"></icon>
              新增漫画
            </button>
            <button class="tab-btn" :class="{ active: mangaChapter.show }" @click="switchToMangaChapter">
              <icon icon="#icon-manga"></icon>
              新增章节
            </button>
          </div>

          <!-- 新增漫画表单 -->
          <form v-if="manga.show" class="modern-form" @submit="addManga">
            <div class="form-row">
              <div class="form-group">
                <label for="manga-title" class="form-label">
                  <span class="required">*</span>标题
                </label>
                <input id="manga-title" v-model="manga.title" class="form-input" placeholder="请输入漫画标题" autocomplete="off" required />
              </div>
              <div class="form-group">
                <label for="manga-title-cn" class="form-label">中文标题</label>
                <input id="manga-title-cn" v-model="manga.titleCn" class="form-input" placeholder="请输入中文标题" autocomplete="off" />
              </div>
              <div class="form-group">
                <label for="manga-author" class="form-label">作者</label>
                <input id="manga-author" v-model="manga.author" class="form-input" placeholder="请输入作者名" autocomplete="off" />
              </div>
            </div>

            <div class="form-group" style="display: none;">
              <label for="manga-description" class="form-label">简介</label>
              <textarea id="manga-description" v-model="manga.description" class="form-textarea" placeholder="请输入漫画简介" autocomplete="off"
                rows="2"></textarea>
            </div>

            <div class="form-group">
              <div class="tag-header">
                <label class="form-label">标签分类</label>
                <div class="tag-mode-toggle">
                  <button type="button" class="toggle-btn" :class="{ active: manga.tagMode === 'input' }" @click="toggleTagMode('input')">
                    输入
                  </button>
                  <button type="button" class="toggle-btn" :class="{ active: manga.tagMode === 'select' }" @click="toggleTagMode('select')">
                    选择
                  </button>
                </div>
              </div>
              <div class="tag-categories-grid">
                <div class="form-row">
                  <div class="form-group">
                    <div class="tag-category">
                      <h5>角色</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.characterInput" class="tag-input" placeholder="输入角色名后按回车添加"
                          @keyup.enter="addCharacterTag" />
                        <button type="button" class="add-tag-btn" @click="addCharacterTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedCharacterTags" :key="'char-' + index"
                            class="selected-tag" @click="removeCharacterTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.characterTags" :key="'char-avail-' + tag.tagId" class="available-tag"
                            :class="{ selected: manga.selectedCharacterTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleCharacterTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <div class="tag-category">
                      <h5>男性</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.maleInput" class="tag-input" placeholder="输入男性标签后按回车添加"
                          @keyup.enter="addMaleTag" />
                        <button type="button" class="add-tag-btn" @click="addMaleTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedMaleTags" :key="'male-sel-' + index"
                            class="selected-tag" @click="removeMaleTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.maleTags" :key="'male-avail-' + tag.tagId" class="available-tag"
                            :class="{ selected: manga.selectedMaleTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleMaleTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <div class="tag-category">
                      <h5>女性</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.femaleInput" class="tag-input" placeholder="输入女性标签后按回车添加"
                          @keyup.enter="addFemaleTag" />
                        <button type="button" class="add-tag-btn" @click="addFemaleTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedFemaleTags" :key="'female-sel-' + index"
                            class="selected-tag" @click="removeFemaleTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.femaleTags" :key="'female-avail-' + tag.tagId" class="available-tag"
                            :class="{ selected: manga.selectedFemaleTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleFemaleTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <div class="tag-category">
                      <h5>混合</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.mixedInput" class="tag-input" placeholder="输入混合标签后按回车添加"
                          @keyup.enter="addMixedTag" />
                        <button type="button" class="add-tag-btn" @click="addMixedTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedMixedTags" :key="'mixed-sel-' + index"
                            class="selected-tag" @click="removeMixedTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.mixedTags" :key="'mixed-avail-' + tag.tagId" class="available-tag"
                            :class="{ selected: manga.selectedMixedTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleMixedTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <div class="tag-category">
                      <h5>其他</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.otherInput" class="tag-input" placeholder="输入其他标签后按回车添加"
                          @keyup.enter="addOtherTag" />
                        <button type="button" class="add-tag-btn" @click="addOtherTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedOtherTags" :key="'other-sel-' + index"
                            class="selected-tag" @click="removeOtherTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.otherTags" :key="'other-avail-' + tag.tagId" class="available-tag"
                            :class="{ selected: manga.selectedOtherTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleOtherTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="form-group">
                    <div class="tag-category">
                      <h5>原作</h5>
                      <div v-if="manga.tagMode === 'input'" class="tag-input-container">
                        <input v-model="manga.originalInput" class="tag-input" placeholder="输入原作名后按回车添加"
                          @keyup.enter="addOriginalTag" />
                        <button type="button" class="add-tag-btn" @click="addOriginalTag">
                          添加
                        </button>
                      </div>
                      <div class="tag-display-area">
                        <div class="selected-tags">
                          <span v-for="(tag, index) in manga.selectedOriginalTags" :key="'original-sel-' + index"
                            class="selected-tag" @click="removeOriginalTag(tag)">
                            {{ tag.tagName }}
                          </span>
                        </div>
                        <div v-if="manga.tagMode === 'select'" class="available-tags">
                          <div v-for="tag in manga.originalTags" :key="'original-avail-' + tag.tagId"
                            class="available-tag"
                            :class="{ selected: manga.selectedOriginalTags.some(selected => selected.tagId === tag.tagId) }"
                            @click="toggleOriginalTag(tag)">
                            {{ tag.tagName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 漫画文件上传和提交按钮 -->
            <div class="form-row" style="grid-template-columns: 1fr 1fr;">
              <div class="form-group">
                <label class="form-label">
                  <span class="required">*</span>漫画文件
                </label>
                <div class="custom-upload" @click="triggerFileInput" @dragover.prevent="handleDragOver"
                  @dragenter.prevent="handleDragEnter" @dragleave.prevent="handleDragLeave"
                  @drop.prevent="handleFileDrop">
                  <input id="manga-upload" ref="fileInput" type="file" accept=".zip" style="display: none;"
                    @change="handleFileSelect" />
                  <div class="upload-content">
                    <div class="upload-icon">
                      <icon icon="#icon-upload"></icon>
                    </div>
                    <div class="upload-text">
                      <p class="primary-text">拖拽漫画文件到此处上传</p>
                      <p class="secondary-text">或 <em>点击选择文件</em></p>
                      <p class="hint-text">仅支持 ZIP 格式，单个文件不超过 500MB</p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-group upload-submit-group">
                <div v-if="manga.selectedFile !== null" class="file-list" style="width: 80%;">
                  <div class="file-item">
                    <span class="file-name">{{ manga.selectedFile.name }}</span>
                    <button type="button" class="remove-btn" @click="removeMangaFile">
                      <icon icon="#icon-delete"></icon>
                    </button>
                  </div>
                </div>
                <div class="form-actions">
                  <span class="error-message" v-if="manga.error">{{ manga.error }}</span>
                  <button type="submit" class="submit-btn">
                    <icon icon="#icon-upload"></icon>
                    上传漫画
                  </button>
                </div>
              </div>
            </div>
          </form>

          <!-- 新增漫画章节表单 -->
          <form v-if="mangaChapter.show" class="modern-form" @submit="addMangaChapter">
            <div class="form-row" style="grid-template-columns: 1fr 1fr;">
              <div class="form-group">
                <label for="manga-title" class="form-label">
                  <span class="required">*</span>漫画
                </label>
                <el-select id="manga-title" v-model="mangaChapter.mangaId" class="manga-select"
                  filterable remote reserve-keyword clearable placeholder="输入标题搜索漫画"
                  :remote-method="searchMangas" :loading="mangaChapter.searching"
                  @visible-change="handleMangaSelectVisible">
                  <el-option v-for="option in mangaChapter.mangaOptions" :key="option.id"
                    :label="mangaOptionLabel(option)" :value="option.id" />
                </el-select>
              </div>

              <div class="form-group">
                <label for="manga-chapter-title" class="form-label">
                  <span class="required">*</span>章节标题
                </label>
                <input id="manga-chapter-title" v-model="mangaChapter.title" class="form-input" placeholder="请输入章节标题" autocomplete="off"
                  required />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">
                <span class="required">*</span>漫画文件
              </label>
              <!-- <div> -->
              <div class="custom-upload" @click="triggerChapterFileInput" @dragover.prevent="handleChapterDragOver"
                @dragenter.prevent="handleChapterDragEnter" @dragleave.prevent="handleChapterDragLeave"
                @drop.prevent="handleChapterFileDrop">
                <input id="chapter-upload" type="file" accept=".zip" style="display: none;"
                  @change="handleChapterFileSelect" />
                <div class="upload-content">
                  <div class="upload-icon">
                    <icon icon="#icon-upload"></icon>
                  </div>
                  <div class="upload-text">
                    <p class="primary-text">拖拽章节文件到此处上传</p>
                    <p class="secondary-text">或 <em>点击选择文件</em></p>
                    <p class="hint-text">仅支持 ZIP 格式，单个文件不超过 500MB</p>
                  </div>
                </div>
              </div>

              <div v-if="mangaChapter.selectedFile !== null" class="file-list">
                <div class="file-item">
                  <span class="file-name">{{ mangaChapter.selectedFile.name }}</span>
                  <button type="button" class="remove-btn" @click="removeMangaChapterFile">
                    <icon icon="#icon-delete"></icon>
                  </button>
                </div>
              </div>
              <!-- </div> -->
            </div>

            <div class="form-actions">
              <span class="error-message" v-if="mangaChapter.error">{{ mangaChapter.error }}</span>
              <button type="submit" class="submit-btn" :disabled="mangaChapter.uploading">
                <icon icon="#icon-upload"></icon>
                {{ mangaChapter.uploading ? '上传中...' : '上传章节' }}
              </button>
            </div>
          </form>
        </div>
      </template>

      <template v-if="drawer.active === 5">
        <div class="coming-soon">
          <icon icon="#icon-anime" class="coming-icon"></icon>
          <h3>动画上传功能</h3>
          <p>即将推出，敬请期待...</p>
        </div>
      </template>

      <!-- 游戏上传 -->
      <template v-if="drawer.active === 3">
        <div class="game-section">
          <!-- 游戏上传表单 -->
          <form class="modern-form" @submit="addGame">
            <div class="form-row">
              <div class="form-group">
                <label for="game-name" class="form-label">
                  <span class="required">*</span>游戏名称
                </label>
                <input id="game-name" v-model="game.name" class="form-input" placeholder="请输入游戏名称" autocomplete="off" required />
              </div>
              <div class="form-group">
                <label for="game-chinese-title" class="form-label">
                  中文名称
                </label>
                <input id="game-chinese-title" v-model="game.chineseTitle" class="form-input" placeholder="请输入中文名称" autocomplete="off" />
              </div>
              <div class="form-group">
                <label for="game-version" class="form-label">
                  版本号
                </label>
                <input id="game-version" v-model="game.version" class="form-input" placeholder="请输入版本号" autocomplete="off" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-row"  style="grid-template-columns: 1fr 1fr;">
                <div class="form-group">
                  <label class="form-label">
                    <span class="required">*</span>游戏封面
                  </label>
                  <div class="game-cover-container">
                    <!-- 隐藏的文件输入框，始终存在 -->
                    <input id="game-cover-upload" type="file" accept="image/*" style="display: none;"
                      @change="handleGameCoverSelect" />
                    <div v-if="!game.coverFile" class="cover-upload-card" @click="triggerGameCoverInput" 
                      @dragover.prevent="handleGameCoverDragOver" @dragenter.prevent="handleGameCoverDragEnter" 
                      @dragleave.prevent="handleGameCoverDragLeave" @drop.prevent="handleGameCoverDrop">
                      <div class="upload-placeholder">
                        <icon icon="#icon-upload" class="upload-icon"></icon>
                        <div class="upload-text">
                          <p>点击上传游戏封面</p>
                          <p class="upload-hint">支持 JPG、PNG 格式</p>
                        </div>
                      </div>
                    </div>
                    <div v-else class="cover-avatar" @click="triggerGameCoverInput" 
                      @dragover.prevent="handleGameCoverDragOver" @dragenter.prevent="handleGameCoverDragEnter" 
                      @dragleave.prevent="handleGameCoverDragLeave" @drop.prevent="handleGameCoverDrop">
                      <img :src="game.coverPreviewUrl" alt="游戏封面" class="cover-avatar-img" />
                      <div class="cover-avatar-actions">
                        <div class="action-btn" @click.stop="triggerGameCoverInput" title="更换封面">
                          <icon icon="#icon-replace"></icon>
                        </div>
                        <div class="action-btn delete-btn" @click.stop="removeGameCover" title="删除封面">
                          <icon icon="#icon-delete"></icon>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">
                    游戏图标
                  </label>
                  <div class="game-cover-container">
                    <!-- TODO 上传游戏图标，还需要重写事件函数、文件处理上传、后端处理保存 -->
                    <!-- 隐藏的文件输入框，始终存在 -->
                    <input id="game-icon-upload" type="file" accept="image/*" style="display: none;"
                      @change="handleGameIconSelect" />
                    <div v-if="!game.iconFile" class="cover-upload-card" @click="triggerGameIconInput" 
                      @dragover.prevent="handleGameIconDragOver" @dragenter.prevent="handleGameIconDragEnter" 
                      @dragleave.prevent="handleGameIconDragLeave" @drop.prevent="handleGameIconDrop">
                      <div class="upload-placeholder">
                        <icon icon="#icon-upload" class="upload-icon"></icon>
                        <div class="upload-text">
                          <p>点击上传游戏封面</p>
                          <p class="upload-hint">支持 JPG、PNG 格式</p>
                        </div>
                      </div>
                    </div>
                    <div v-else class="cover-avatar" @click="triggerGameIconInput" 
                      @dragover.prevent="handleGameIconDragOver" @dragenter.prevent="handleGameIconDragEnter" 
                      @dragleave.prevent="handleGameIconDragLeave" @drop.prevent="handleGameIconDrop">
                      <img :src="game.iconPreviewUrl" alt="游戏图标" class="cover-avatar-img" />
                      <div class="cover-avatar-actions">
                        <div class="action-btn" @click.stop="triggerGameIconInput" title="更换图标">
                          <icon icon="#icon-replace"></icon>
                        </div>
                        <div class="action-btn delete-btn" @click.stop="removeGameIcon" title="删除图标">
                          <icon icon="#icon-delete"></icon>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-group" style="grid-column: 2 / 4;">
                <label for="game-description" class="form-label">
                  游戏简介
                </label>
                <textarea id="game-description" v-model="game.description" class="form-textarea" placeholder="请输入游戏简介" rows="4" autocomplete="off"></textarea>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">
                游戏预览
              </label>
              <div class="game-preview-wall">
                <div class="preview-upload-card" @click="triggerGamePreviewInput" 
                  @dragover.prevent="handleGamePreviewDragOver" @dragenter.prevent="handleGamePreviewDragEnter" 
                  @dragleave.prevent="handleGamePreviewDragLeave" @drop.prevent="handleGamePreviewDrop">
                  <input id="game-preview-upload" ref="gamePreviewInput" type="file" accept="image/*" multiple style="display: none;"
                    @change="handleGamePreviewSelect" />
                  <div class="upload-placeholder">
                      <icon icon="#icon-upload" class="upload-icon"></icon>
                      <div class="upload-text">
                        <p>点击添加预览图片</p>
                        <p class="upload-hint">支持 JPG、PNG 格式</p>
                      </div>
                    </div>
                </div>
                <div v-for="(preview, index) in game.previewPreviews" :key="'preview-' + index" class="preview-card">
                  <img :src="preview.url" alt="游戏预览" class="preview-card-img" @click="previewImage(preview.url)" />
                  <div class="preview-card-actions">
                    <div class="action-btn preview-btn" @click="previewImage(preview.url)" title="预览">
                      <icon icon="#icon-view"></icon>
                    </div>
                    <div class="action-btn delete-btn" @click="removeGamePreview(index)" title="删除">
                      <icon icon="#icon-delete"></icon>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 图片预览对话框 -->
              <div v-if="showImagePreview" class="image-preview-dialog" @click="closeImagePreview">
                <div class="preview-content" @click.stop>
                  <img :src="previewImageUrl" alt="预览图片" class="preview-full-image" />
                  <button class="close-preview-btn" @click="closeImagePreview">
                    <icon icon="#icon-close"></icon>
                  </button>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <span class="error-message" v-if="game.error">{{ game.error }}</span>
              <button type="submit" class="submit-btn">
                <icon icon="#icon-upload"></icon>
                上传游戏
              </button>
            </div>
          </form>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch } from "vue";
import { useStore } from 'vuex'
import server from '@/util/request';
import { ElMessage } from 'element-plus';
import uploadLoadingUrl from '../assets/icon/upload/loading.svg';

export default {
  name: "Acg17Upload",
  setup() {
    const store = useStore()

    // drawer 控制
    const drawer = reactive({
      show: false,
      active: 0,
    });
    const catalogList = [
      { index: 1, title: "上传插画", icon: "#icon-illustration", },
      { index: 2, title: "上传漫画", icon: "#icon-manga", },
      // { index: 3, title: "上传动画", icon: "#icon-anime", },
      { index: 3, title: "上传游戏", icon: "#icon-game", },
      { index: 4, title: "上传小说", icon: "#icon-novel", }
    ]
    function loadNovelTags() {
      server.get('/novel-tag/getList')
        .then(res => {
          novel.tagList = res.data
        })
        .catch(err => {
          ElMessage.error('获取标签列表失败【' + err + '】。')
        })
    }
    function showDrawer(index) {
      drawer.show = true
      drawer.active = index
      if (index === 4) {
        novel.show = true
        chapter.show = false
        loadNovelTags()
      } else if (index === 2) {
        // 漫画上传初始化
        manga.show = true
        mangaChapter.show = false
        manga.error = ''
        mangaChapter.error = ''
        // 获取漫画标签
        const sortTagsByCount = (tags) => (tags || []).slice().sort((a, b) => (b.tagCount || 0) - (a.tagCount || 0))
        server.get('/manga-tag/list')
          .then(res => {
            manga.characterTags = sortTagsByCount(res.data.characterTags)
            manga.maleTags = sortTagsByCount(res.data.maleTags)
            manga.femaleTags = sortTagsByCount(res.data.femaleTags)
            manga.mixedTags = sortTagsByCount(res.data.mixedTags)
            manga.otherTags = sortTagsByCount(res.data.otherTags)
            manga.originalTags = sortTagsByCount(res.data.originalTags)
          })
          .catch(err => {
            ElMessage.error('获取漫画标签失败【' + err + '】，请重试')
          })
      }
    }
    // 监听打开上传面板
    watch(() => store.state.uploadDrawer.show, (newValue, oldValue) => {
      if (newValue && !oldValue) {
        drawer.show = true
        drawer.active = store.state.uploadDrawer.active
        // 上传小说
        if (store.state.uploadDrawer.active === 4) {
          if (store.state.uploadDrawer.subActive === 0) {
            novel.show = true
            chapter.show = false
            loadNovelTags()
          } else {
            novel.show = false
            chapter.show = true
            chapter.novelId = store.state.uploadDrawer.data.novelId
            chapter.novelTitle = store.state.uploadDrawer.data.novelTitle
          }
        }
      }
      store.commit('closeUploadDrawer')
    })
    // 获取对话框标题
    function getDialogTitle() {
      const titles = {
        1: '上传插画',
        2: '上传漫画',
        3: '上传动画',
        4: '上传小说',
        5: '上传游戏'
      }
      return titles[drawer.active] || '上传作品'
    }

    // ####################################### 插画上传 #######################################
    // 插画上传
    const illustration = reactive({
      accept: 'image/*',  // 仅允许上传图片文件
      action: '',
      list: [], // 上传图像列表
    })

    // 上传前
    function beforeUpload(file) {
      // 检查文件大小
      if (file.size > 100 * 1024 * 1024) {
        ElMessage.warning('文件大小不能超过 100MB')
        return false
      }
      // 上传成功前暂时使用文件uid区别不同图像
      illustration.list.push({ id: file.uid + '', url: uploadLoadingUrl })
      return true
    }

    // 自定义上传
    function customUpload(e) {
      const formData = new FormData()
      formData.append('file', e.file);
      // 传到后端
      server.post('/illustration/upload', formData)
        .then(res => {
          // 更新图像路径，显示图像
          for (let i = 0; i < illustration.list.length; i++) {
            if (illustration.list[i].id === e.file.uid + '') {
              illustration.list[i].id = res.data.id
              illustration.list[i].url = res.data.urlTiny
              break
            }
          }
        })
        .catch(err => {
          ElMessage.error('上传失败【' + err + '】，请重试')
          for (let i = 0; i < illustration.list.length; i++) {
            if (illustration.list[i].id === e.file.uid + '') {
              illustration.list[i].url = ''
              break
            }
          }
        })
    }

    // 移除图片
    function removeImage(id) {
      const index = illustration.list.findIndex(item => item.id === id)
      if (index > -1) {
        illustration.list.splice(index, 1)
      }
    }

    // ####################################### 小说上传 #######################################
    // 小说上传
    const novel = reactive({
      show: false, // 是否显示（当前是新增小说，还是新增章节）
      title: '', // 表单项，书名
      author: '', // 表单项，作者
      tags: [], // 表单项，标签，（已选择的）
      tagList: [], // 标签，（全部的）
      error: '', // 表单校验错误信息
      addingTag: false, // 是否正在添加新标签
      newTagName: '', // 新标签名称
    })
    // 切换到新增书籍
    function switchToNovel() {
      novel.show = true
      chapter.show = false
      loadNovelTags()
    }
    // 切换到新增章节
    function switchToChapter() {
      novel.show = false
      chapter.show = true
    }
    // 表单校验
    function checkNovelForm(e) {
      if (novel.title === '') {
        e.target[0].focus()
        novel.error = '「书名」不能为空'
      } else {
        return true
      }
      return false
    }
    // 新增书籍
    function addNovel(e) {
      e.preventDefault()
      // 表单校验，不通过直接拒绝
      if (!checkNovelForm(e)) {
        return false
      }
      novel.error = ''
      // 上传小说
      const data = {
        title: novel.title,
        author: novel.author,
        tags: novel.tags
      }
      server.post('/novel/addNovel', data)
        .then(res => {
          ElMessage.success(`新增小说「${res.data.title}」成功。`)
          novel.show = false
          chapter.show = true
          chapter.novelId = res.data.id
          chapter.novelTitle = res.data.title
        })
        .catch(err => {
          ElMessage.error(`新增小说「${novel.title}」失败【${err}】。`)
        })
      return false
    }
    // 切换标签选择
    function toggleTag(tagName) {
      const index = novel.tags.indexOf(tagName)
      if (index > -1) {
        novel.tags.splice(index, 1)
      } else {
        novel.tags.push(tagName)
      }
    }
    // 开始添加新标签
    function startAddTag() {
      novel.addingTag = true
      novel.newTagName = ''
    }
    // 确认添加新标签
    function confirmAddTag() {
      const tagName = novel.newTagName.trim()
      if (tagName && !novel.tagList.some(tag => tag.name === tagName)) {
        novel.tagList.push({ id: `new-${tagName}`, name: tagName })
        novel.tags.push(tagName)
      }
      novel.addingTag = false
      novel.newTagName = ''
    }
    // 取消添加新标签
    function cancelAddTag() {
      novel.addingTag = false
      novel.newTagName = ''
    }
    // 小说章节上传
    const chapter = reactive({
      show: false, // 是否显示（当前是新增小说，还是新增章节）
      novelId: '', // 小说id
      novelTitle: '', // 小说名
      title: '', // 表单项，章节名
      content: '', // 表单项，章节内容
      error: '' // 表单校验，错误信息
    })
    // 表单校验
    function checkChapterForm(e) {
      if (chapter.title === '') {
        e.target[1].focus()
        chapter.error = '章节名不能为空'
      } else if (chapter.content === '') {
        e.target[2].focus()
        chapter.error = '内容不能为空'
      } else {
        return true
      }
      return false
    }
    // 上传章节
    function addChapter(e) {
      e.preventDefault()
      // 表单校验
      if (!checkChapterForm(e)) {
        return
      }
      chapter.error = ''
      // 上传
      const data = {
        novelId: chapter.novelId,
        title: chapter.title,
        content: chapter.content.split(/\r?\n\s*\r?\n/).filter(p => p.trim() !== ''),
      }
      server.post('/novel-chapter/addChapter', data)
        .then(() => {
          ElMessage.success(`新增章节「${data.title}」成功。`)
          chapter.title = ''
          chapter.content = ''
        })
        .catch(err => {
          ElMessage.error(`新增章节「${data.title}」失败【${err}】。`)
        })
      return false
    }

    // ####################################### 漫画上传 #######################################
    // 漫画
    const manga = reactive({
      show: true, // 是否显示新增漫画表单
      title: '', // 标题
      titleCn: '', // 中文标题
      description: '', // 简介
      author: '', // 作者
      original: '', // 原作
      selectedFile: null, // 选择的文件对象
      error: '', // 错误信息
      dragCounter: 0, // 拖拽计数器
      tagMode: 'input', // 标签模式：'input' 添加模式，'select' 选择模式
      // 标签分类-输入
      characterInput: '',
      maleInput: '',
      femaleInput: '',
      mixedInput: '',
      otherInput: '',
      originalInput: '',
      // 标签分类-选择
      characterTags: [],
      maleTags: [],
      femaleTags: [],
      mixedTags: [],
      otherTags: [],
      originalTags: [],
      // 标签分类-已选择
      selectedCharacterTags: [],
      selectedMaleTags: [],
      selectedFemaleTags: [],
      selectedMixedTags: [],
      selectedOtherTags: [],
      selectedOriginalTags: []
    })
    // 漫画章节
    const mangaChapter = reactive({
      show: false, // 是否显示新增章节表单
      mangaId: null, // 漫画ID
      mangaOptions: [], // 可选择的漫画
      searching: false, // 是否正在搜索漫画
      uploading: false, // 是否正在上传章节
      title: '', // 章节标题
      selectedFile: null, // 选择的文件对象
      error: '', // 错误信息
      dragCounter: 0 // 拖拽计数器
    })

    // 切换标签模式
    function toggleTagMode(mode) {
      manga.tagMode = mode
    }

    // ############### 【漫画上传】相关函数 ###############
    // 触发文件选择
    function triggerFileInput() {
      const input = document.querySelector('#manga-upload')
      if (input) input.click()
    }
    // 处理文件选择
    function handleFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        processSelectedFile(file)
      }
    }
    // 处理拖拽进入
    function handleDragEnter(event) {
      event.preventDefault()
      manga.dragCounter++
      if (manga.dragCounter === 1) {
        event.currentTarget.classList.add('dragover')
      }
    }
    // 处理拖拽悬停
    function handleDragOver(event) {
      event.preventDefault()
    }
    // 处理拖拽离开
    function handleDragLeave(event) {
      event.preventDefault()
      manga.dragCounter--
      if (manga.dragCounter === 0) {
        event.currentTarget.classList.remove('dragover')
      }
    }
    // 处理拖拽文件
    function handleFileDrop(event) {
      event.preventDefault()
      manga.dragCounter = 0
      event.currentTarget.classList.remove('dragover')
      const files = event.dataTransfer.files
      if (files.length > 0) {
        processSelectedFile(files[0])
      }
    }
    // 处理选择的文件
    function processSelectedFile(file) {
      // 文件类型验证
      const isValidType = file.type === 'application/zip' || file.name.toLowerCase().endsWith('.zip')
      const isValidSize = file.size / 1024 / 1024 < 500

      if (!isValidType) {
        ElMessage.error('只能上传 ZIP 格式的文件！')
        return
      }
      if (!isValidSize) {
        ElMessage.error('文件大小不能超过 500MB！')
        return
      }

      // 保存文件对象，在addManga时统一上传
      manga.selectedFile = file

      ElMessage.success('文件已选择，点击提交按钮进行上传')
    }
    // 移除漫画文件
    function removeMangaFile() {
      manga.selectedFile = null
    }

    // ############### 【漫画上传表单】相关函数 ###############
    // 漫画表单验证
    function checkMangaForm(e) {
      if (manga.title === '') {
        e.target[0].focus()
        manga.error = '「标题」不能为空'
        return false
      }
      return true
    }
    // 提交漫画
    function addManga(e) {
      e.preventDefault()
      // 表单验证
      if (!checkMangaForm(e)) {
        return false
      }
      manga.error = ''

      // 使用FormData统一上传文本数据和文件
      const formData = new FormData()
      // 添加文本字段
      formData.append('title', manga.title)
      formData.append('chineseTitle', manga.titleCn || '')
      formData.append('description', manga.description || '')
      formData.append('author', manga.author || '')
      const selectedTags = [
        ...manga.selectedCharacterTags.map(tag => ({ ...tag, category: 1 })),
        ...manga.selectedMaleTags.map(tag => ({ ...tag, category: 2 })),
        ...manga.selectedFemaleTags.map(tag => ({ ...tag, category: 3 })),
        ...manga.selectedMixedTags.map(tag => ({ ...tag, category: 4 })),
        ...manga.selectedOtherTags.map(tag => ({ ...tag, category: 5 })),
        ...manga.selectedOriginalTags.map(tag => ({ ...tag, category: 6 }))
      ]
      formData.append('tags', JSON.stringify(selectedTags))
      // 添加文件（只在有文件时才添加）
      if (manga.selectedFile) {
        formData.append('file', manga.selectedFile)
      }
      // 上传
      server.post('/manga/addManga', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(res => {
          ElMessage.success(`新增漫画「${res.data}」成功。`)
          // 重置表单
          manga.title = ''
          manga.titleCn = ''
          manga.description = ''
          manga.author = ''
          manga.original = ''
          manga.characterInput = ''
          manga.maleInput = ''
          manga.femaleInput = ''
          manga.mixedInput = ''
          manga.otherInput = ''
          manga.originalInput = ''
          manga.selectedFile = null
          manga.dragCounter = 0
          manga.selectedCharacterTags = []
          manga.selectedMaleTags = []
          manga.selectedFemaleTags = []
          manga.selectedMixedTags = []
          manga.selectedOtherTags = []
          manga.selectedOriginalTags = []
          drawer.show = false
        })
        .catch(err => {
          manga.error = '上传失败【' + err + '】，请重试'
        })

      return false
    }

    // ############### 【切换表单】相关函数 ###############
    // 切换到新增漫画
    function switchToManga() {
      manga.show = true
      mangaChapter.show = false
      manga.error = ''
    }
    // 切换到新增章节
    function switchToMangaChapter() {
      manga.show = false
      mangaChapter.show = true
      mangaChapter.error = ''
      if (mangaChapter.mangaOptions.length === 0) {
        searchMangas('')
      }
    }

    let mangaSearchSequence = 0
    function searchMangas(query) {
      const sequence = ++mangaSearchSequence
      mangaChapter.searching = true
      server.get('/manga/list', {
        params: {
          pageNum: 1,
          deleted: false,
          title: query ? query.trim() : undefined
        }
      })
        .then(res => {
          if (sequence === mangaSearchSequence) {
            mangaChapter.mangaOptions = res.data?.records || []
          }
        })
        .catch(err => {
          if (sequence === mangaSearchSequence) {
            mangaChapter.error = '搜索漫画失败【' + err + '】，请重试'
          }
        })
        .finally(() => {
          if (sequence === mangaSearchSequence) {
            mangaChapter.searching = false
          }
        })
    }
    function handleMangaSelectVisible(visible) {
      if (visible && mangaChapter.mangaOptions.length === 0 && !mangaChapter.searching) {
        searchMangas('')
      }
    }
    function mangaOptionLabel(option) {
      const chineseTitle = option.chineseTitle && option.chineseTitle !== option.title
        ? `（${option.chineseTitle}）`
        : ''
      return `${option.title}${chineseTitle} #${option.id}`
    }

    // ############### 【标签分类】相关函数 ###############
    // 全局标签ID计数器，用于生成唯一的临时标签ID
    let tempTagIdCounter = -1
    // 生成唯一的临时标签ID
    function generateTempTagId() {
      return tempTagIdCounter--
    }
    // 通用标签处理函数：支持"#"分割多个标签
    function processTagInput(input) {
      if (!input) return []

      // 检查是否包含"#"符号
      if (input.includes('#')) {
        // 按"#"分割，过滤空字符串，去除首尾空格
        return input.split('#')
          .map(tag => tag.trim())
          .filter(tag => tag.length > 0)
      } else {
        // 单个标签，去除首尾空格
        const trimmed = input.trim()
        return trimmed ? [trimmed] : []
      }
    }

    // 添加角色标签
    function addCharacterTag() {
      const input = manga.characterInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedCharacterTags.some(tag => tag.tagName === tagName)) {
          // 创建新的标签对象，使用唯一的临时ID
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedCharacterTags.push(newTag)
        }
      })
      manga.characterInput = ''
    }
    // 移除角色标签
    function removeCharacterTag(tag) {
      const index = manga.selectedCharacterTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedCharacterTags.splice(index, 1)
      }
    }
    // 切换角色标签选择
    function toggleCharacterTag(tag) {
      const index = manga.selectedCharacterTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedCharacterTags.splice(index, 1)
      } else {
        manga.selectedCharacterTags.push(tag)
      }
    }

    // 添加男性标签
    function addMaleTag() {
      const input = manga.maleInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedMaleTags.some(tag => tag.tagName === tagName)) {
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedMaleTags.push(newTag)
        }
      })
      manga.maleInput = ''
    }
    // 移除男性标签
    function removeMaleTag(tag) {
      const index = manga.selectedMaleTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedMaleTags.splice(index, 1)
      }
    }
    // 切换男性标签选择
    function toggleMaleTag(tag) {
      const index = manga.selectedMaleTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedMaleTags.splice(index, 1)
      } else {
        manga.selectedMaleTags.push(tag)
      }
    }

    // 添加女性标签
    function addFemaleTag() {
      const input = manga.femaleInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedFemaleTags.some(tag => tag.tagName === tagName)) {
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedFemaleTags.push(newTag)
        }
      })
      manga.femaleInput = ''
    }
    // 移除女性标签
    function removeFemaleTag(tag) {
      const index = manga.selectedFemaleTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedFemaleTags.splice(index, 1)
      }
    }
    // 切换女性标签选择
    function toggleFemaleTag(tag) {
      const index = manga.selectedFemaleTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedFemaleTags.splice(index, 1)
      } else {
        manga.selectedFemaleTags.push(tag)
      }
    }

    // 添加混合标签
    function addMixedTag() {
      const input = manga.mixedInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedMixedTags.some(tag => tag.tagName === tagName)) {
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedMixedTags.push(newTag)
        }
      })
      manga.mixedInput = ''
    }
    // 移除混合标签
    function removeMixedTag(tag) {
      const index = manga.selectedMixedTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedMixedTags.splice(index, 1)
      }
    }
    // 切换混合标签选择
    function toggleMixedTag(tag) {
      const index = manga.selectedMixedTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedMixedTags.splice(index, 1)
      } else {
        manga.selectedMixedTags.push(tag)
      }
    }

    // 添加其他标签
    function addOtherTag() {
      const input = manga.otherInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedOtherTags.some(tag => tag.tagName === tagName)) {
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedOtherTags.push(newTag)
        }
      })
      manga.otherInput = ''
    }
    // 移除其他标签
    function removeOtherTag(tag) {
      const index = manga.selectedOtherTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedOtherTags.splice(index, 1)
      }
    }
    // 切换其他标签选择
    function toggleOtherTag(tag) {
      const index = manga.selectedOtherTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedOtherTags.splice(index, 1)
      } else {
        manga.selectedOtherTags.push(tag)
      }
    }

    // 添加原作标签
    function addOriginalTag() {
      const input = manga.originalInput.trim()
      if (!input) return

      const tagNames = processTagInput(input)
      tagNames.forEach(tagName => {
        if (!manga.selectedOriginalTags.some(tag => tag.tagName === tagName)) {
          const newTag = { tagId: generateTempTagId(), tagName: tagName, tagCount: 0 }
          manga.selectedOriginalTags.push(newTag)
        }
      })
      manga.originalInput = ''
    }
    // 移除原作标签
    function removeOriginalTag(tag) {
      const index = manga.selectedOriginalTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedOriginalTags.splice(index, 1)
      }
    }
    // 切换原作标签选择
    function toggleOriginalTag(tag) {
      const index = manga.selectedOriginalTags.findIndex(t => t.tagId === tag.tagId)
      if (index > -1) {
        manga.selectedOriginalTags.splice(index, 1)
      } else {
        manga.selectedOriginalTags.push(tag)
      }
    }

    // ############### 【漫画章节上传】相关函数 ###############
    // 触发章节文件选择
    function triggerChapterFileInput() {
      const input = document.querySelector('#chapter-upload')
      if (input) input.click()
    }
    // 处理章节文件选择
    function handleChapterFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        processChapterSelectedFile(file)
      }
    }
    // 处理章节拖拽进入
    function handleChapterDragEnter(event) {
      event.preventDefault()
      mangaChapter.dragCounter++
      if (mangaChapter.dragCounter === 1) {
        event.currentTarget.classList.add('dragover')
      }
    }
    // 处理章节拖拽悬停
    function handleChapterDragOver(event) {
      event.preventDefault()
    }
    // 处理章节拖拽离开
    function handleChapterDragLeave(event) {
      event.preventDefault()
      mangaChapter.dragCounter--
      if (mangaChapter.dragCounter === 0) {
        event.currentTarget.classList.remove('dragover')
      }
    }
    // 处理拖拽章节文件
    function handleChapterFileDrop(event) {
      event.preventDefault()
      mangaChapter.dragCounter = 0
      event.currentTarget.classList.remove('dragover')
      const files = event.dataTransfer.files
      if (files.length > 0) {
        processChapterSelectedFile(files[0])
      }
    }
    // 处理选择的章节文件
    function processChapterSelectedFile(file) {
      // 文件类型验证
      const isValidType = file.type === 'application/zip' || file.name.toLowerCase().endsWith('.zip')
      const isValidSize = file.size / 1024 / 1024 < 500

      if (!isValidType) {
        ElMessage.error('只能上传 ZIP 格式的文件！')
        return
      }
      if (!isValidSize) {
        ElMessage.error('文件大小不能超过 500MB！')
        return
      }

      // 保存文件对象，在addMangaChapter时统一上传
      mangaChapter.selectedFile = file

      ElMessage.success('章节文件已选择，点击提交按钮进行上传')
    }
    // 移除章节文件
    function removeMangaChapterFile() {
      mangaChapter.selectedFile = null
      const input = document.querySelector('#chapter-upload')
      if (input) input.value = ''
    }

    // ############### 【漫画章节上传表单】相关函数 ###############
    // 漫画章节表单验证
    function checkMangaChapterForm() {
      if (!mangaChapter.mangaId) {
        mangaChapter.error = '请选择漫画'
        return false
      }
      if (!mangaChapter.title || !mangaChapter.title.trim()) {
        mangaChapter.error = '请输入章节标题'
        return false
      }
      if (!mangaChapter.selectedFile) {
        mangaChapter.error = '请选择「漫画章节文件」'
        return false
      }
      return true
    }
    // 提交漫画章节
    function addMangaChapter(e) {
      e.preventDefault()
      if (mangaChapter.uploading) {
        return false
      }
      // 表单验证
      if (!checkMangaChapterForm()) {
        return false
      }
      mangaChapter.error = ''

      // 使用FormData统一上传文本数据和文件
      const formData = new FormData()
      formData.append('title', mangaChapter.title.trim())
      // 添加文件
      formData.append('file', mangaChapter.selectedFile)

      mangaChapter.uploading = true
      server.post(`/manga/${mangaChapter.mangaId}/chapters`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(res => {
          ElMessage.success(`新增漫画章节「${res.data.title}」成功。`)
          // 重置表单
          mangaChapter.mangaId = null
          mangaChapter.title = ''
          mangaChapter.selectedFile = null
          mangaChapter.dragCounter = 0
          const input = document.querySelector('#chapter-upload')
          if (input) input.value = ''
          drawer.show = false
        })
        .catch(err => {
          mangaChapter.error = '上传失败【' + err + '】，请重试'
        })
        .finally(() => {
          mangaChapter.uploading = false
        })

      return false
    }

    // ####################################### 游戏上传 #######################################
    // 游戏上传
    const game = reactive({
      name: '', // 游戏名称
      chineseTitle: '', // 中文名称
      version: '', // 版本号
      description: '', // 游戏简介
      coverFile: null, // 游戏封面文件
      coverPreviewUrl: '', // 游戏封面预览URL
      iconFile: null, // 游戏图标文件
      iconPreviewUrl: '', // 游戏图标预览URL
      previewFiles: [], // 游戏预览文件列表
      previewPreviews: [], // 游戏预览图片预览URL列表
      error: '', // 表单校验错误信息
    })

    // 图片预览相关状态
    const showImagePreview = ref(false)
    const previewImageUrl = ref('')

    // 预览图片
    function previewImage(url) {
      previewImageUrl.value = url
      showImagePreview.value = true
    }

    // 关闭图片预览
    function closeImagePreview() {
      showImagePreview.value = false
      previewImageUrl.value = ''
    }

    // 触发游戏封面文件选择
    function triggerGameCoverInput() {
      const input = document.querySelector('#game-cover-upload')
      if (input) input.click()
    }

    // 处理游戏封面文件选择
    function handleGameCoverSelect(event) {
      const file = event.target.files[0]
      if (file) {
        processGameCoverFile(file)
      }
    }

    // 处理游戏封面拖拽事件
    function handleGameCoverDragEnter(event) {
      event.preventDefault()
      event.currentTarget.classList.add('dragover')
    }

    function handleGameCoverDragOver(event) {
      event.preventDefault()
    }

    function handleGameCoverDragLeave(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
    }

    function handleGameCoverDrop(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
      const files = event.dataTransfer.files
      if (files.length > 0) {
        processGameCoverFile(files[0])
      }
    }

    // 处理游戏封面文件
    function processGameCoverFile(file) {
      // 文件类型验证
      const isValidType = file.type.startsWith('image/')
      const isValidSize = file.size / 1024 / 1024 < 10

      if (!isValidType) {
        ElMessage.error('只能上传图片文件！')
        return
      }
      if (!isValidSize) {
        ElMessage.error('文件大小不能超过 10MB！')
        return
      }

      // 释放之前的预览URL
      if (game.coverPreviewUrl) {
        URL.revokeObjectURL(game.coverPreviewUrl)
      }

      game.coverFile = file
      game.coverPreviewUrl = URL.createObjectURL(file)
      ElMessage.success('游戏封面已选择')
    }

    // 移除游戏封面
    function removeGameCover() {
      if (game.coverPreviewUrl) {
        URL.revokeObjectURL(game.coverPreviewUrl)
      }
      game.coverFile = null
      game.coverPreviewUrl = ''
    }

    // 触发游戏封面文件选择
    function triggerGameIconInput() {
      const input = document.querySelector('#game-icon-upload')
      if (input) input.click()
    }

    // 处理游戏封面文件选择
    function handleGameIconSelect(event) {
      const file = event.target.files[0]
      if (file) {
        processGameIconFile(file)
      }
    }

    // 处理游戏封面拖拽事件
    function handleGameIconDragEnter(event) {
      event.preventDefault()
      event.currentTarget.classList.add('dragover')
    }

    function handleGameIconDragOver(event) {
      event.preventDefault()
    }

    function handleGameIconDragLeave(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
    }

    function handleGameIconDrop(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
      const files = event.dataTransfer.files
      if (files.length > 0) {
        processGameIconFile(files[0])
      }
    }

    // 处理游戏封面文件
    function processGameIconFile(file) {
      // 文件类型验证
      const isValidType = file.type.startsWith('image/')
      const isValidSize = file.size / 1024 / 1024 < 1
      if (!isValidType) {
        ElMessage.error('只能上传图片文件！')
        return
      }
      if (!isValidSize) {
        ElMessage.error('文件大小不能超过 10MB！')
        return
      }

      // 释放之前的预览URL
      if (game.iconPreviewUrl) {
        URL.revokeObjectURL(game.iconPreviewUrl)
      }

      game.iconFile = file
      game.iconPreviewUrl = URL.createObjectURL(file)
      ElMessage.success('游戏图标已选择')
    }

    // 移除游戏图标
    function removeGameIcon() {
      if (game.iconPreviewUrl) {
        URL.revokeObjectURL(game.iconPreviewUrl)
      }
      game.iconFile = null
      game.iconPreviewUrl = ''
    }

    // 触发游戏预览文件选择
    function triggerGamePreviewInput() {
      const input = document.querySelector('#game-preview-upload')
      if (input) input.click()
    }

    // 处理游戏预览文件选择
    function handleGamePreviewSelect(event) {
      const files = Array.from(event.target.files)
      files.forEach(file => {
        processGamePreviewFile(file)
      })
    }

    // 处理游戏预览拖拽事件
    function handleGamePreviewDragEnter(event) {
      event.preventDefault()
      event.currentTarget.classList.add('dragover')
    }

    function handleGamePreviewDragOver(event) {
      event.preventDefault()
    }

    function handleGamePreviewDragLeave(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
    }

    function handleGamePreviewDrop(event) {
      event.preventDefault()
      event.currentTarget.classList.remove('dragover')
      const files = Array.from(event.dataTransfer.files)
      files.forEach(file => {
        processGamePreviewFile(file)
      })
    }

    // 处理游戏预览文件
    function processGamePreviewFile(file) {
      // 文件类型验证
      const isValidType = file.type.startsWith('image/')
      const isValidSize = file.size / 1024 / 1024 < 10

      if (!isValidType) {
        ElMessage.error('只能上传图片文件！')
        return
      }
      if (!isValidSize) {
        ElMessage.error('文件大小不能超过 10MB！')
        return
      }

      game.previewFiles.push(file)
      game.previewPreviews.push({
        file: file,
        url: URL.createObjectURL(file)
      })
      ElMessage.success('游戏预览图片已添加')
    }

    // 移除游戏预览
    function removeGamePreview(index) {
      // 释放预览URL
      if (game.previewPreviews[index]) {
        URL.revokeObjectURL(game.previewPreviews[index].url)
      }
      game.previewFiles.splice(index, 1)
      game.previewPreviews.splice(index, 1)
    }

    // 游戏表单验证
    function checkGameForm() {
      if (game.name === '') {
        game.error = '「游戏名称」不能为空'
        return false
      }
      if (!game.coverFile) {
        game.error = '请选择「游戏封面」'
        return false
      }
      return true
    }

    // 提交游戏
    async function addGame(e) {
      e.preventDefault()
      // 表单验证
      if (!checkGameForm()) {
        return false
      }
      game.error = ''

      try {
        // 使用FormData统一上传文本数据和文件
        const formData = new FormData()
        // 添加文本字段 - 映射到后端DTO字段
        formData.append('title', game.name)  // 前端name -> 后端title
        formData.append('chineseTitle', game.chineseTitle)  // 中文名称
        formData.append('version', game.version)
        formData.append('description', game.description)
        // 添加封面文件
        formData.append('cover', game.coverFile)  // 前端coverFile -> 后端cover
        // 添加游戏图标
        if (game.iconFile) {
          formData.append('icon', game.iconFile)  // 前端iconFile -> 后端icon
        }
        // 添加预览文件
        game.previewFiles.forEach((file) => {
          formData.append('previewImages', file)  // 前端previewFiles -> 后端previewImages
        })

        // 调用后端接口
        const response = await server.post('/game/addGame', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        if (response.code === 200) {
          ElMessage.success(`游戏「${game.name}」上传成功！`)
          
          // 重置表单
          // 清理预览URL
          if (game.coverPreviewUrl) {
            URL.revokeObjectURL(game.coverPreviewUrl)
          }
          if (game.iconPreviewUrl) {
            URL.revokeObjectURL(game.iconPreviewUrl)
          }
          game.previewPreviews.forEach(preview => {
            URL.revokeObjectURL(preview.url)
          })
          
          game.name = ''
          game.chineseTitle = ''
          game.version = ''
          game.description = ''
          game.coverFile = null
          game.coverPreviewUrl = ''
          game.iconFile = null
          game.iconPreviewUrl = ''
          game.previewFiles = []
          game.previewPreviews = []
          drawer.show = false
        } else {
          ElMessage.error(response.data.message || '上传失败')
        }
      } catch (error) {
        console.error('上传游戏失败:', error)
        ElMessage.error('上传游戏失败: ' + (error.response?.data?.message || error.message))
      }

      return false
    }

    return {
      drawer, catalogList, showDrawer, getDialogTitle,
      illustration, beforeUpload, customUpload, removeImage,
      novel, addNovel, toggleTag, startAddTag, confirmAddTag, cancelAddTag, switchToNovel,
      chapter, addChapter, switchToChapter,
      manga, addManga, removeMangaFile, switchToManga, triggerFileInput, handleFileSelect, handleDragEnter, handleDragOver, handleDragLeave, handleFileDrop, processSelectedFile, toggleTagMode,
      addCharacterTag, removeCharacterTag, toggleCharacterTag,
      addMaleTag, removeMaleTag, toggleMaleTag,
      addFemaleTag, removeFemaleTag, toggleFemaleTag,
      addMixedTag, removeMixedTag, toggleMixedTag,
      addOtherTag, removeOtherTag, toggleOtherTag,
      addOriginalTag, removeOriginalTag, toggleOriginalTag,
      mangaChapter, addMangaChapter, removeMangaChapterFile, switchToMangaChapter, searchMangas, handleMangaSelectVisible, mangaOptionLabel, triggerChapterFileInput, handleChapterFileSelect, handleChapterDragEnter, handleChapterDragOver, handleChapterDragLeave, handleChapterFileDrop, processChapterSelectedFile,
      game, addGame,
      triggerGameCoverInput, handleGameCoverSelect, handleGameCoverDragEnter, handleGameCoverDragOver, handleGameCoverDragLeave, handleGameCoverDrop, removeGameCover,
      triggerGameIconInput, handleGameIconSelect, handleGameIconDragEnter, handleGameIconDragOver, handleGameIconDragLeave, handleGameIconDrop, removeGameIcon,
      triggerGamePreviewInput, handleGamePreviewSelect, handleGamePreviewDragEnter, handleGamePreviewDragOver, handleGamePreviewDragLeave, handleGamePreviewDrop, removeGamePreview, showImagePreview, previewImageUrl, previewImage, closeImagePreview,
    };
  }
}
</script>

<style scoped>
.btn-upload {
  margin: 0;
  padding: 9px 24px;
  border: none;
  border-radius: 100vw;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s ease 0s, color 0.2s ease 0s;
  color: rgba(0, 0, 0, 0.64);
  background-color: rgba(0, 0, 0, 0.04);
}

.btn-upload .btn-upload-content {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 4px;
  align-items: center;
}

.btn-upload .btn-upload-content .icon {
  width: 10px;
  height: 10px;
  fill: rgba(0, 0, 0, 0.64);
}

.btn-upload-mobile {
  width: 40px;
  height: 40px;
  padding: 7px;
  margin: 0 -7px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s ease 0s, color 0.2s ease 0s;
  background-color: transparent;
}

.btn-upload-mobile:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.btn-upload-mobile .icon {
  width: 26px;
  height: 26px;
  fill: rgb(133, 133, 133);
}

@media screen and (min-width:580px) {
  .btn-upload-mobile {
    display: none;
  }
}

@media not screen and (min-width:580px) {
  .btn-upload {
    display: none;
  }
}

.el-popper .item {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 8px;
  align-items: center;
}

.el-popper .item .icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

/* 对话框样式 */
.upload-dialog {
  border-radius: 8px;
  overflow: hidden;
}

.upload-dialog ::v-deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 24px;
  margin: 0;
}

.upload-dialog ::v-deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.upload-dialog ::v-deep(.el-dialog__close) {
  color: white;
  font-size: 20px;
}

.upload-dialog ::v-deep(.el-dialog__body) {
  padding: 0;
}

.dialog-content {
  padding: 0 24px 24px;
  min-height: 400px;
}

/* 插画上传区域样式 */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.upload-area {
  width: 100%;
  position: relative;
}

.upload-area ::v-deep(.el-upload) {
  width: 100%;
}

.upload-area ::v-deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.upload-area ::v-deep(.el-upload-dragger:hover) {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.15);
}

.upload-area ::v-deep(.el-upload-dragger.is-dragover) {
  border-color: #4f46e5;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  transform: scale(1.02);
  box-shadow: 0 12px 35px rgba(79, 70, 229, 0.25);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 24px;
  text-align: center;
  position: relative;
  z-index: 2;
}

.upload-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(102, 126, 234, 0.05) 0%, transparent 70%);
  z-index: -1;
}

.upload-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.upload-icon .icon {
  width: 24px;
  height: 24px;
  fill: white;
}

.upload-area:hover .upload-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-text .primary-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
}

.upload-text .secondary-text {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.upload-text .secondary-text em {
  color: #667eea;
  font-style: normal;
  font-weight: 600;
  text-decoration: underline;
  text-decoration-color: rgba(102, 126, 234, 0.3);
  text-underline-offset: 2px;
}

.upload-text .hint-text {
  font-size: 12px;
  color: #94a3b8;
  margin: 8px 0 0 0;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 已上传图片展示 */
.uploaded-images {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e2e8f0;
}

.uploaded-images h4 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploaded-images h4::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.image-item {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 1;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.image-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.image-item:hover img {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0.4) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(2px);
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.remove-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
  position: relative;
}

.remove-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
  pointer-events: none;
}

.remove-btn:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.6);
}

.remove-btn:active {
  transform: scale(0.95);
}

.remove-btn .icon {
  width: 18px;
  height: 18px;
  fill: currentColor;
  z-index: 1;
}

/* 小说上传区域 */
.novel-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tab-header {
  display: flex;
  background: #f8fafc;
  border-radius: 8px;
  padding: 4px;
  gap: 4px;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: #e2e8f0;
  color: #475569;
}

.tab-btn.active {
  background: white;
  color: #1e293b;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* 现代表单样式 */
.modern-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 4px;
}

.required {
  color: #ef4444;
  font-weight: 600;
}

.form-input {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.form-input:hover {
  border-color: #9ca3af;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.manga-select {
  width: 100%;
}

.form-textarea {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
}

.form-textarea:hover {
  border-color: #9ca3af;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-select {
  width: 100%;
}

.form-select ::v-deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d1d5db;
  transition: all 0.3s ease;
}

.form-select ::v-deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #9ca3af;
}

.form-select ::v-deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #667eea, 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 标签选择器 */
.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 13px;
  color: #6b7280;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.tag-item:hover {
  border-color: #667eea;
  color: #667eea;
}

.tag-item.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

.tag-item .check-icon {
  width: 14px;
  height: 14px;
  fill: currentColor;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.tag-item.active .check-icon {
  opacity: 1;
}

/* 标签输入区域 */
.tag-input-section {
  margin-bottom: 16px;
}

.tag-input-section .label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.tag-input-container {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.tag-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.add-tag-btn {
  border: none;
  border-radius: 6px;
  background: #667eea;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.add-tag-btn:hover {
  background: #5a67d8;
}

/* 已选标签区域 */
.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 24px;
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
}

.selected-tag {
  padding: 3px 6px;
  background: #667eea;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.remove-tag-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.remove-tag-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 可选标签区域 */
.available-tags {
  max-height: 210px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
}

.available-tags::-webkit-scrollbar {
  width: 6px;
}

.available-tags::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.available-tag {
  padding: 2px 6px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #ffffff;
  color: #374151;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.available-tag:hover {
  border-color: #667eea;
  background: #f0f4ff;
  color: #667eea;
}

.available-tag.selected {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

/* 表单操作区域 */
.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.error-message {
  color: #ef4444;
  font-size: 13px;
  font-weight: 500;
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.submit-btn:active {
  transform: translateY(0);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  transform: none;
}

.submit-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* 即将推出功能样式 */
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.coming-icon {
  width: 64px;
  height: 64px;
  fill: #d1d5db;
  margin-bottom: 20px;
}

.coming-soon h3 {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
}

.coming-soon p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 新增标签样式 */
.tag-item.add-tag-btn {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 13px;
  padding: 8px 16px;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.tag-item.add-tag-btn:hover {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  border-color: #667eea;
  color: #667eea;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.1);
}

.tag-item.add-tag-input {
  padding: 6px 8px;
  gap: 6px;
  background: white;
  border: 1px solid #667eea;
}

.tag-item.add-tag-input input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #374151;
  width: 80px;
  padding: 0;
  line-height: 1.2;
}

.tag-item.add-tag-input input::placeholder {
  color: #9ca3af;
}

.tag-item.add-tag-btn .add-icon,
.tag-item.add-tag-input .confirm-icon,
.tag-item.add-tag-input .cancel-icon {
  width: 14px;
  height: 14px;
  cursor: pointer;
  transition: all 0.2s;
  fill: #6b7280;
}

.tag-item.add-tag-input .confirm-icon:hover {
  fill: #10b981;
}

.tag-item.add-tag-input .cancel-icon:hover {
  fill: #ef4444;
}

/* 漫画上传区域 */
.manga-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.game-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 游戏封面容器 - Element Plus Avatar 风格 */
.game-cover-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-upload-card {
  width: 100%;
  height: 130px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafcff;
  position: relative;
  overflow: hidden;
}

.cover-upload-card:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.cover-upload-card.dragover {
  border-color: #409eff;
  background-color: #d9ecff;
}

/* 防止子元素干扰拖拽事件 */
.cover-upload-card * {
  pointer-events: none;
}

.cover-upload-card {
  pointer-events: auto;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.upload-placeholder .upload-icon {
  width: 24px;
  height: 24px;
  fill: #c0c4cc;
}

.upload-text {
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.upload-text p {
  margin: 0;
  line-height: 1.4;
}

.upload-hint {
  color: #c0c4cc;
  font-size: 11px;
}

.cover-avatar {
  position: relative;
  width: 100%;
  height: 130px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.3s;
}

.cover-avatar:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.cover-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.cover-avatar-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.cover-avatar:hover .cover-avatar-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #606266;
}

.action-btn:hover {
  background: #fff;
  transform: scale(1.1);
}

.action-btn.delete-btn:hover {
  color: #f56c6c;
}

.action-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* 游戏预览照片墙 - Element Plus Upload 照片墙风格 */
.game-preview-wall {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.preview-upload-card {
  box-sizing:border-box;
  width: 220px;
  height: 150px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafcff;
  position: relative;
  overflow: hidden;
  order: -1;
}

.preview-upload-card:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.preview-upload-card.dragover {
  border-color: #409eff;
  background-color: #d9ecff;
}

.preview-upload-card .upload-placeholder {
  color: #8c939d;
}

.preview-upload-card .upload-icon {
  width: 28px;
  height: 28px;
  fill: #c0c4cc;
}

/* 防止子元素干扰拖拽事件 */
.preview-upload-card * {
  pointer-events: none;
}

.preview-upload-card {
  pointer-events: auto;
}

.preview-card {
  box-sizing:border-box;
  position: relative;
  width: 220px;
  height: 150px;
  border: 1px solid #c0c4cc;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
}

.preview-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.preview-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.preview-card:hover .preview-card-img {
  transform: scale(1.05);
}

.preview-card-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-card:hover .preview-card-actions {
  opacity: 1;
}

.preview-btn:hover {
  color: #409eff;
}

/* 图片预览对话框 */
.image-preview-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  cursor: pointer;
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  cursor: default;
}

.preview-full-image {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.close-preview-btn {
  position: absolute;
  top: -40px;
  right: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-preview-btn:hover {
  background: #fff;
  color: #409eff;
}

.close-preview-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.upload-submit-group {
  display: flex;
  align-items: center;
  justify-content: center;
  /* height: 120px; */
}

.upload-submit-group .form-actions {
  margin: 0;
}

/* 上传内容样式 */
.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 16px;
  text-align: center;
}

.upload-icon {
  width: 32px;
  height: 32px;
  fill: #667eea;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.upload-icon .icon {
  width: 32px;
  height: 32px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.primary-text {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  line-height: 1.4;
}

.secondary-text {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.3;
}

.secondary-text em,
.secondary-text .highlight {
  color: #667eea;
  font-style: normal;
  font-weight: 500;
  text-decoration: underline;
  text-decoration-color: rgba(102, 126, 234, 0.3);
}

.hint-text {
  font-size: 11px;
  color: #9ca3af;
  margin: 0;
  line-height: 1.2;
  margin-top: 2px;
}

/* 标签分类样式 */
.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tag-mode-toggle {
  display: flex;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  gap: 2px;
}

.toggle-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #64748b;
}

.toggle-btn:hover {
  color: #475569;
  background: rgba(255, 255, 255, 0.5);
}

.toggle-btn.active {
  background: white;
  color: #667eea;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tag-categories {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 12px;
}

.tag-category {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.tag-category h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.tag-category .tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-category .tag-item {
  padding: 6px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #6b7280;
  user-select: none;
}

.tag-category .tag-item:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
}

.tag-category .tag-item.active {
  background: #667eea;
  border-color: #667eea;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
}

/* 漫画自定义上传组件样式 */
.custom-upload {
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
  overflow: hidden;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-upload::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(102, 126, 234, 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.custom-upload:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.15);
}

.custom-upload:hover::before {
  opacity: 1;
}

.custom-upload:active {
  transform: translateY(-1px) scale(0.98);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
}

/* 拖拽状态样式 */
.custom-upload.dragover {
  border-color: #4f46e5;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  transform: scale(1.02);
  box-shadow: 0 12px 35px rgba(79, 70, 229, 0.25);
}

.custom-upload.dragover::before {
  opacity: 1;
}

/* 防止子元素干扰拖拽事件 */
.custom-upload * {
  pointer-events: none;
}

.custom-upload {
  pointer-events: auto;
}

/* 文件列表样式 */
.file-list {
  margin-top: 16px;
}

.file-item {
  display: grid;
  grid-template-columns: auto 40px;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  width: 16px;
  height: 16px;
  color: #6b7280;
}

.file-name {
  color: #374151;
  font-weight: 500;
}

.file-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.file-status.ready {
  background: #dbeafe;
  color: #1d4ed8;
}

.remove-file-btn {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.remove-file-btn:hover {
  background: #fee2e2;
}

.remove-btn {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-btn:hover {
  background: #fee2e2;
}

.remove-btn .icon {
  width: 16px;
  height: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-dialog {
    width: 95vw !important;
    margin: 20px auto;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }

  .upload-area ::v-deep(.el-upload-dragger) {
    height: 160px;
    padding: 16px;
  }

  .upload-content {
    padding: 16px;
  }

  .upload-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 12px;
  }

  .upload-icon .icon {
    width: 20px;
    height: 20px;
  }

  .upload-text .primary-text {
    font-size: 16px;
  }

  .upload-text .secondary-text {
    font-size: 13px;
  }

  .upload-text .hint-text {
    font-size: 11px;
    padding: 6px 12px;
  }

  .uploaded-images {
    padding: 16px;
  }

  .uploaded-images h4 {
    font-size: 16px;
  }

  .custom-upload {
    padding: 24px 16px;
    min-height: 140px;
  }

  .custom-upload .upload-icon {
    width: 40px;
    height: 40px;
    margin-bottom: 12px;
  }

  .custom-upload .upload-icon .icon {
    width: 20px;
    height: 20px;
  }

  .custom-upload .primary-text {
    font-size: 16px;
  }

  .custom-upload .secondary-text {
    font-size: 13px;
  }

  .custom-upload .hint-text {
    font-size: 11px;
    padding: 6px 12px;
  }

  .tab-header {
    flex-direction: column;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .tag-categories {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .tag-category {
    padding: 12px;
  }
}
</style>
